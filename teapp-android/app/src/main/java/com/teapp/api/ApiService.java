package com.teapp.api;

import com.teapp.model.ActivityItem;
import com.teapp.model.ActivityRequest;
import com.teapp.model.ActivityStep;
import com.teapp.model.AuthResponse;
import com.teapp.model.ChangePasswordRequest;
import com.teapp.model.Child;
import com.teapp.model.ChildRequest;
import com.teapp.model.CompletionRequest;
import com.teapp.model.LoginRequest;
import com.teapp.model.RegisterRequest;
import com.teapp.model.ScheduleEntry;
import com.teapp.model.ScheduleEntryRequest;
import com.teapp.model.TherapistInfo;
import com.teapp.model.WeeklySchedule;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest body);

    @POST("auth/forgot-password")
    Call<Void> forgotPassword(@Body Map<String, String> body);

    // ── Children ──────────────────────────────────────────────────────────────
    @GET("children")
    Call<List<Child>> getChildren();

    @GET("children/{id}")
    Call<Child> getChild(@Path("id") String id);

    @POST("children")
    Call<Child> createChild(@Body ChildRequest body);

    @PUT("children/{id}")
    Call<Child> updateChild(@Path("id") String id, @Body ChildRequest body);

    @DELETE("children/{id}")
    Call<Void> deleteChild(@Path("id") String id);

    @PUT("children/{id}/avatar")
    Call<Void> updateChildAvatar(@Path("id") String id, @Body RequestBody body);

    // ── Schedule ──────────────────────────────────────────────────────────────
    @GET("children/{childId}/schedule")
    Call<WeeklySchedule> getSchedule(@Path("childId") String childId);

    @POST("children/{childId}/schedule")
    Call<ScheduleEntry> addEntry(@Path("childId") String childId,
                                 @Body ScheduleEntryRequest body);

    @PUT("children/{childId}/schedule/{entryId}")
    Call<ScheduleEntry> updateEntry(@Path("childId") String childId,
                                    @Path("entryId") String entryId,
                                    @Body ScheduleEntryRequest body);

    @DELETE("children/{childId}/schedule/{entryId}")
    Call<Void> deleteEntry(@Path("childId") String childId,
                           @Path("entryId") String entryId);

    // ── Completions ───────────────────────────────────────────────────────────
    @POST("children/{childId}/schedule/{entryId}/completions")
    Call<Void> markCompleted(@Path("childId") String childId,
                             @Path("entryId") String entryId,
                             @Body CompletionRequest body);

    @DELETE("children/{childId}/schedule/{entryId}/completions")
    Call<Void> unmarkCompleted(@Path("childId") String childId,
                               @Path("entryId") String entryId,
                               @Body CompletionRequest body);

    @DELETE("children/{childId}/completions/current-week")
    Call<Void> resetWeek(@Path("childId") String childId);

    // ── Activities ────────────────────────────────────────────────────────────
    @GET("activities")
    Call<List<ActivityItem>> getActivities();

    @GET("activities")
    Call<List<ActivityItem>> getActivitiesByCategory(@Query("category") String category);

    @GET("activities/predefined")
    Call<List<ActivityItem>> getPredefinedActivities();

    @POST("activities")
    Call<ActivityItem> createActivity(@Body ActivityRequest body);

    @PUT("activities/{id}")
    Call<ActivityItem> updateActivity(@Path("id") String id, @Body ActivityRequest body);

    @DELETE("activities/{id}")
    Call<Void> deleteActivity(@Path("id") String id);

    @GET("activities/{id}/steps")
    Call<List<ActivityStep>> getSteps(@Path("id") String activityId);

    @PUT("activities/{id}/steps")
    Call<List<ActivityStep>> saveSteps(@Path("id") String activityId,
                                       @Body List<com.teapp.model.ActivityStepRequest> steps);

    // ── User ──────────────────────────────────────────────────────────────────
    @GET("users/me")
    Call<AuthResponse> getMe();

    @PUT("users/me")
    Call<AuthResponse> updateProfile(@Body RequestBody body);

    @PUT("users/me/password")
    Call<Void> changePassword(@Body ChangePasswordRequest body);

    @DELETE("users/me")
    Call<Void> deleteMe();

    // ── Therapist ─────────────────────────────────────────────────────────────
    @GET("therapist/supervised")
    Call<List<Map<String, Object>>> getSupervisedParents();

    @GET("therapist/supervised/{parentId}/children")
    Call<List<Child>> getSupervisedChildren(@Path("parentId") String parentId);

    @GET("therapist/supervised/{parentId}/children/{childId}/schedule")
    Call<WeeklySchedule> getSupervisedSchedule(@Path("parentId") String parentId,
                                               @Path("childId") String childId);

    @GET("therapist/my-therapists")
    Call<List<TherapistInfo>> getMyTherapists();

    @POST("therapist/link")
    Call<Void> linkTherapist(@Body Map<String, String> body);

    @DELETE("therapist/link/{therapistId}")
    Call<Void> unlinkTherapist(@Path("therapistId") String therapistId);

    // ── User ──────────────────────────────────────────────────────────────────
    @PUT("users/me/avatar")
    Call<Void> updateUserAvatar(@Body RequestBody body);
}
