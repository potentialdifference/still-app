package uk.org.potentialdifference.stillapp.imageservice.webservice;

import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by russell on 07/11/2015.
 */
public interface StillAppService {

    @Multipart
    @POST("private")
    Call<StillAppResponse> uploadPrivateFile(@Header("Authorization") String authCode, @Query("uid") String userId, @Query("tag") String tag, @Part("image\"; filename=\"image.jpg\" ") RequestBody image);

    @GET("public/{path}")
    Call<ResponseBody> getPublicFile(@Path("path") String path);

}
