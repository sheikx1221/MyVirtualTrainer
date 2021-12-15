package com.virtualtrainer.intro;
import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.virtualtrainer.DAO.DAOProfil;
import com.virtualtrainer.DAO.Profile;
import com.virtualtrainer.DatePickerDialogFragment;
import com.virtualtrainer.MainActivity;
import com.virtualtrainer.app.R;
import com.virtualtrainer.utils.DateConverter;
import com.virtualtrainer.utils.Gender;
import com.heinrichreimersoftware.materialintro.app.SlideFragment;
import com.onurkaganaldemir.ktoastlib.KToast;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class NewProfileFragment extends SlideFragment {
    private EditText mName;
    private EditText mSize;
    private EditText mBirthday;
    private Button mBtCreate;
    private RadioButton mRbMale;
    private RadioButton mRbFemale;
    private RadioButton mRbOtherGender;
    private ImageButton bSignIn;
    private GoogleSignInAccount user;
    private Boolean LoginComplete = false;
    GoogleSignInClient mGoogleSignInClient;

    private boolean mProfilCreated = false;
    private final View.OnClickListener clickCreateButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Initialisation des objets DB
            DAOProfil mDbProfils = new DAOProfil(v.getContext());
            if (LoginComplete == false) {
                KToast.warningToast(getActivity(), "Sign Into Google First!", Gravity.BOTTOM, KToast.LENGTH_SHORT);
                return;
            }
            if (mName.getText().toString().isEmpty()) {
                //Toast.makeText(getActivity().getBaseContext(), R.string.fillAllFields, Toast.LENGTH_SHORT).show();
                KToast.warningToast(getActivity(), getResources().getText(R.string.fillNameField).toString(), Gravity.BOTTOM, KToast.LENGTH_SHORT);
            } else {
                int size = 0;
                try {
                    if (!mSize.getText().toString().isEmpty()) {
                        size = Double.valueOf(mSize.getText().toString()).intValue();
                    }
                } catch (NumberFormatException e) {
                    size = 0;
                }

                int lGender = Gender.UNKNOWN;
                if (mRbMale.isChecked()) {
                    lGender = Gender.MALE;
                } else if (mRbFemale.isChecked()) {
                    lGender = Gender.FEMALE;
                } else if (mRbOtherGender.isChecked()) {
                    lGender = Gender.OTHER;
                }

                Profile p = new Profile(mName.getText().toString(), size, DateConverter.editToDate(mBirthday.getText().toString()), lGender, user.getPhotoUrl().toString());
                // Create the new profil
                mDbProfils.addProfil(p);
                //Toast.makeText(getActivity().getBaseContext(), R.string.profileCreated, Toast.LENGTH_SHORT).show();

                new SweetAlertDialog(getContext(), SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText(p.getName())
                    .setContentText(getContext().getResources().getText(R.string.profileCreated).toString())
                    .setConfirmClickListener(sDialog -> nextSlide())
                    .show();
                mProfilCreated = true;
            }
        }
    };
    private DatePickerDialogFragment mDateFrag = null;
    private MainActivity motherActivity;
    private OnDateSetListener dateSet = new OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            mBirthday.setText(DateConverter.dateToString(year, month + 1, day));
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(mBirthday.getWindowToken(), 0);
        }
    };
    private Intent signInIntent = new Intent();
    private String TAG = "MVT";

    public NewProfileFragment() {
        // Required empty public constructor
    }
    public static NewProfileFragment newInstance() {
        return new NewProfileFragment();
    }
    private void showDatePickerFragment() {
        if (mDateFrag == null) {
            mDateFrag = DatePickerDialogFragment.newInstance(dateSet);
        }

        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        mDateFrag.show(ft, "dialog");
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.introfragment_newprofile, container, false);

        mName = view.findViewById(R.id.profileName);
        mSize = view.findViewById(R.id.profileSize);
        mBirthday = view.findViewById(R.id.profileBirthday);
        mBtCreate = view.findViewById(R.id.create_newprofil);
        mRbMale = view.findViewById(R.id.radioButtonMale);
        mRbFemale = view.findViewById(R.id.radioButtonFemale);
        mRbOtherGender = view.findViewById(R.id.radioButtonOtherGender);
        bSignIn = view.findViewById(R.id.button_google);

        mBirthday.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(mBirthday.getWindowToken(), 0);
                showDatePickerFragment();
            }
        });

        /* Initialisation des boutons */
        mBtCreate.setOnClickListener(clickCreateButton);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build();

        mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);
        bSignIn.setOnClickListener(this.showLoginUI());
        getIntroActivity().addOnNavigationBlockedListener((position, direction) -> {
            //Slide slide = getIntroActivity().getSlide(position);

            if (position == 4) {
                mBtCreate.callOnClick();
            }
        });
        // Inflate the layout for this fragment
        return view;
    }
    private View.OnClickListener showLoginUI() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, 1);
            }
        };
        return listener;
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == 1) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                new SweetAlertDialog(getContext(), SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Google Sign In Successfull")
                    .setContentText("Connected to your Google account")
                    .show();
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                Log.d(TAG, account.toString());
                user = account;
                TextView LoginButton = getView().findViewById(R.id.t_signIn);
                LoginButton.setText("Login Successful");
                mName.setText(user.getDisplayName());
                LoginComplete = true;
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                new SweetAlertDialog(getContext(), SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Google Sign In Failed")
                    .setContentText("Unable to Complete Sign up")
                    .show();
            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public boolean canGoForward() {
        return mProfilCreated;
    }
    public MainIntroActivity getIntroActivity() {
        if (getActivity() instanceof MainIntroActivity) {
            return (MainIntroActivity) getActivity();
        } else {
            throw new IllegalStateException("SlideFragments must be attached to MainIntroActivity.");
        }
    }
}
