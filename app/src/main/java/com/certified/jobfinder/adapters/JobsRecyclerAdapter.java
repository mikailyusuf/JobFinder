package com.certified.jobfinder.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.certified.jobfinder.BusinessActivity;
import com.certified.jobfinder.BusinessProfileActivity;
import com.certified.jobfinder.R;
import com.certified.jobfinder.model.Job;
import com.certified.jobfinder.model.SavedJob;
import com.certified.jobfinder.util.IntentExtra;
import com.certified.jobfinder.util.PreferenceKeys;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.like.LikeButton;
import com.like.OnLikeListener;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class JobsRecyclerAdapter extends FirestoreRecyclerAdapter<Job, JobsRecyclerAdapter.JobsViewHolder> {

    private static final String TAG = "JobsRecyclerAdapter";
    public Context mContext;

    //    Firebase
    public FirebaseFirestore db = FirebaseFirestore.getInstance();
    public FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    public DocumentReference savedJobRef;
    private DocumentReference reference;

    public JobsRecyclerAdapter(@NonNull FirestoreRecyclerOptions<Job> options) {
        super(options);
    }

    @NonNull
    @Override
    public JobsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.jobs_list, parent, false);
        Log.d(TAG, "onCreateViewHolder: view created");

        return new JobsViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull JobsViewHolder holder, int position, @NonNull Job model) {
        Log.d(TAG, "onBindViewHolder: called");

        Uri profileImageUrl = model.getProfile_image_url();

        Glide.with(mContext)
                .load(R.drawable.logo_one)
                .into(holder.ivBusinessProfileImage);
        holder.tvJobTitle.setText(model.getJob_title());
        holder.tvBusinessName.setText(model.getBusiness_name() + " --- " + model.getLocation());
        holder.tvDescription.setText("Description: " + model.getDescription());
        holder.likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                Log.d(TAG, "liked: " + model.getId());
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                SharedPreferences.Editor editor = preferences.edit();

                Set<String> savedJobs = preferences.getStringSet(PreferenceKeys.SAVED_JOBS, new HashSet<>());
                savedJobs.add(model.getId());

                editor.putStringSet(PreferenceKeys.SAVED_JOBS, savedJobs);
                Log.d(TAG, "liked: saved: " + model.getId());
                editor.apply();

                CollectionReference savedJobRef = db.collection("saved_jobs")
                        .document(user.getUid()).collection("my_saved_jobs");
                savedJobRef.get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Set<String> ids = new HashSet<>();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String id = document.getString("original_job_id");
                                    ids.add(id);
                                }
                                if (ids.contains(model.getId())) {
                                    Log.d(TAG, "onComplete: Job already saved: " + model.getId());
                                } else {
                                    DocumentReference saveJob = db.collection("saved_jobs")
                                            .document(user.getUid()).collection("my_saved_jobs").document();

                                    String id = saveJob.getId();
                                    String original_job_id = model.getId();
                                    String business_name = model.getBusiness_name();
                                    String business_email = model.getBusiness_email();
                                    String business_phone = model.getBusiness_phone();
                                    String business_location = model.getBusiness_location();
                                    Uri business_profile_image_url = model.getProfile_image_url();
                                    String job_title = model.getJob_title();
                                    String description = model.getDescription();
                                    String location = model.getLocation();
                                    String requirements = model.getRequirements();
                                    String salary = model.getSalary();
                                    String creator_id = model.getCreator_id();
                                    String uid = user.getUid();

                                    SavedJob savedJob = new SavedJob(id, original_job_id, business_name, business_email,
                                            business_phone, business_location, business_profile_image_url, job_title, description,
                                            location, requirements, salary, null, creator_id, uid);
                                    saveJob.set(savedJob)
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    Log.d(TAG, "onComplete: Job saved: " + saveJob.getId());
                                                }
                                            });
                                }
                            }
                        });
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                Log.d(TAG, "unliked: " + model.getId());
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                SharedPreferences.Editor editor = preferences.edit();

                Set<String> savedJobs = preferences.getStringSet(PreferenceKeys.SAVED_JOBS, new HashSet<>());
                savedJobs.remove(model.getId());

                editor.putStringSet(PreferenceKeys.SAVED_JOBS, savedJobs);
                Log.d(TAG, "unliked: unsaved: " + model.getId());
                editor.apply();

                CollectionReference savedJobColRef = db.collection("saved_jobs")
                        .document(user.getUid()).collection("my_saved_jobs");
                savedJobColRef.get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String originalJobId = document.getString("original_job_id");
                                    String savedJobId = document.getId();
                                    if (originalJobId.equals(model.getId())) {
                                        DocumentReference savedJobDocRef = db.collection("saved_jobs")
                                                .document(user.getUid()).collection("my_saved_jobs").document(savedJobId);
                                        savedJobDocRef.delete()
                                                .addOnCompleteListener(task12 -> {
                                                    if (task12.isSuccessful()) {
                                                        Log.d(TAG, "onComplete: Job " + savedJobId + " successfully deleted");
                                                    }
                                                });
                                    }
                                }
                            }
                        });
            }
        });
        holder.checkIfSaved(model.getId());

        holder.jobDetails.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, BusinessActivity.class);
            mContext.startActivity(intent);
        });
//
        holder.ivBusinessProfileImage.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, BusinessProfileActivity.class);
            intent.putExtra(IntentExtra.BUSINESS_PROFILE_IMAGE, model.getProfile_image_url());
            intent.putExtra(IntentExtra.BUSINESS_NAME, model.getBusiness_name());
            intent.putExtra(IntentExtra.BUSINESS_EMAIL, model.getBusiness_email());
            intent.putExtra(IntentExtra.BUSINESS_PHONE, model.getBusiness_phone());
            intent.putExtra(IntentExtra.BUSINESS_LOCATION, model.getBusiness_location());
            mContext.startActivity(intent);
        });
    }

    public class JobsViewHolder extends RecyclerView.ViewHolder {
        private CardView jobDetails;
        private ImageView ivBusinessProfileImage;
        private TextView tvJobTitle, tvBusinessName, tvDescription;
        public LikeButton likeButton;

        public JobsViewHolder(@NonNull View itemView) {
            super(itemView);
            jobDetails = itemView.findViewById(R.id.job_card_view);
            ivBusinessProfileImage = itemView.findViewById(R.id.business_profile_image);
            tvBusinessName = itemView.findViewById(R.id.tv_business_name_location);
            tvJobTitle = itemView.findViewById(R.id.tv_job_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            likeButton = itemView.findViewById(R.id.likeButton);
            itemView.setOnClickListener(view -> {
            });
        }

        public void checkIfSaved(String id) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            Set<String> savedJobs = preferences.getStringSet(PreferenceKeys.SAVED_JOBS, new HashSet<>());
            if (savedJobs.contains(id)) {
                Log.d(TAG, "onComplete: Job: " + id + " is saved");
                likeButton.setLiked(true);
            }
            CollectionReference savedJobRef = db.collection("saved_jobs").document(user.getUid()).collection("my_saved_jobs");
            savedJobRef.get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String originalJobId = document.getString("original_job_id");
                                if (originalJobId.equals(id)) {
                                    Log.d(TAG, "onComplete: Job: " + id + " is saved");
                                    likeButton.setLiked(true);
                                }
                            }
                        }
                    });
        }
    }
}