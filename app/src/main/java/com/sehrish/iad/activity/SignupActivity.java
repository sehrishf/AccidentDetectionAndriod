package com.sehrish.iad.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.sehrish.iad.R;
import com.sehrish.iad.http.APIClient;
import com.sehrish.iad.http.APIInterface;
import com.sehrish.iad.http.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private APIInterface apiInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_activity);

        apiInterface = APIClient.getClient().create(APIInterface.class);
    }

    public void goToSignInActivity(View v) {
        Intent intent = new Intent(this,  SigninActivity.class);
        startActivity(intent);
    }
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void signUpSave(View view){

        EditText editName  = (EditText) findViewById(R.id.editName);
        EditText editEmail  = (EditText) findViewById(R.id.editEmail);
        EditText editPhone  = (EditText) findViewById(R.id.editPhone);
        EditText editPass  = (EditText) findViewById(R.id.editPass);

        String nameText=editName.getText().toString();
        String phoneText=editPhone.getText().toString();
        String emailText=editEmail.getText().toString();
        String passText=editPass.getText().toString();

        User userData = new User(0,phoneText,nameText,emailText,passText);
        Call<User> call1 = apiInterface.saveUserData(userData);

        call1.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {

                User respondData = response.body();
                if(respondData.getUserId() == 0)
                {
                    Toast.makeText(getBaseContext(), "Phone number already exist ", Toast.LENGTH_SHORT).show();
return;
                }
                goToSignInActivity(view);
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getBaseContext(), "Check internet connection or Server might be in maintanence", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });

    }
}