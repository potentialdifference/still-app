package uk.org.potentialdifference.stillapp.nodefs;





import com.squareup.okhttp.RequestBody;

import java.util.HashMap;

import retrofit.Call;

import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by russell on 26/10/2015.
 */
public interface NodeFSService {

    @POST("{key}/file/{dir}/{name}")
    Call<NodeFSResponse> createFile(@Path("key") String key, @Path("dir") String dir, @Path("name") String name);

    @POST("{key}/dir/{directory}")
    Call<NodeFSResponse> createDirectory(@Path("key") String key, @Path("directory") String dir);

    @PUT("{key}/save/{dir}/{name}")
    Call<NodeFSResponse> saveFileContents(@Path("key") String key, @Path("dir") String dir, @Path("name") String name,@Body RequestBody body);


}
