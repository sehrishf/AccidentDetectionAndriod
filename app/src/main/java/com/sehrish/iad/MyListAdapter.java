package com.sehrish.iad;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sehrish.iad.R;
import com.sehrish.iad.http.APIClient;
import com.sehrish.iad.http.APIInterface;
import com.sehrish.iad.http.model.RemoveContact;
import com.sehrish.iad.http.model.User;
import com.sehrish.iad.http.model.UserFriend;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private List<String> listPhonenumber;
    private List<Integer> listPhoneNumberId;
    private APIInterface apiInterface;

    public MyListAdapter(Activity context, List<String> maintitle, List<Integer> phoneNumberIds) {

        super(context, R.layout.friends_list, maintitle);

        apiInterface = APIClient.getClient().create(APIInterface.class);
        this.context = context;
        this.listPhonenumber = maintitle;
        this.listPhoneNumberId = phoneNumberIds;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.friends_list, null, true);

        TextView titleText = (TextView) rowView.findViewById(R.id.phonenumber);
        Button button = (Button) rowView.findViewById(R.id.btnDeleteNumber);
        titleText.setText(listPhonenumber.get(position));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = titleText.getText().toString();
                int friendId = listPhoneNumberId.get(position);

                listPhonenumber.remove(position);
                listPhoneNumberId.remove(position);

                SharedPreferences sharedPreferences = context.getSharedPreferences("user_details", Context.MODE_PRIVATE);
                String userId = sharedPreferences.getString("userid", "");

                RemoveContact removeContact = new RemoveContact();
                removeContact.setUserId(Integer.parseInt(userId));
                removeContact.setFriendId(friendId);

                Call<List<UserFriend>> call = apiInterface.removeFriend(removeContact);
                call.enqueue(new Callback<List<UserFriend>>() {
                    @Override
                    public void onResponse(Call<List<UserFriend>> call, Response<List<UserFriend>> response) {


                    }
                    @Override
                    public void onFailure(Call<List<UserFriend>> call, Throwable t) {
                        call.cancel();
                    }
                });

                notifyDataSetChanged();
            }
        });

        return rowView;
    }

    ;
}
