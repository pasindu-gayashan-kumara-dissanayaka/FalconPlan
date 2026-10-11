package lk.falconplan.app.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.data.common.Resource;
import lk.falconplan.app.data.model.UserProfile;
import lk.falconplan.app.data.repository.AuthRepository;
import lk.falconplan.app.data.repository.AuthRepositoryImpl;
import lk.falconplan.app.data.repository.UserRepository;
import lk.falconplan.app.data.repository.UserRepositoryImpl;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<Resource<FirebaseUser>> authStateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<UserProfile>> userProfileLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> operationStatusLiveData = new MutableLiveData<>();

    public AuthViewModel() {
        this.authRepository = new AuthRepositoryImpl();
        this.userRepository = new UserRepositoryImpl();
    }

    public AuthViewModel(AuthRepository authRepository, UserRepository userRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
    }

    public LiveData<Resource<FirebaseUser>> getAuthStateLiveData() { return authStateLiveData; }
    public LiveData<Resource<UserProfile>> getUserProfileLiveData() { return userProfileLiveData; }
    public LiveData<Resource<String>> getOperationStatusLiveData() { return operationStatusLiveData; }

    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    public boolean isSignedIn() {
        return authRepository.isSignedIn();
    }

    public boolean isEmailVerified() {
        return authRepository.isEmailVerified();
    }

    public void loginWithEmail(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            operationStatusLiveData.setValue(Resource.error("Please enter email and password.", null));
            return;
        }

        authStateLiveData.setValue(Resource.loading(null));
        authRepository.signInWithEmailAndPassword(email.trim(), password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                authStateLiveData.setValue(Resource.success(user));
                loadProfile(user.getUid());
            }

            @Override
            public void onError(Exception exception) {
                authStateLiveData.setValue(Resource.error(exception.getLocalizedMessage(), null));
            }
        });
    }

    public void registerWithEmail(String name, String email, String password, String activeMode) {
        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            operationStatusLiveData.setValue(Resource.error("Please fill in all required fields.", null));
            return;
        }

        if (password.length() < 6) {
            operationStatusLiveData.setValue(Resource.error("Password must be at least 6 characters long.", null));
            return;
        }

        authStateLiveData.setValue(Resource.loading(null));
        authRepository.createUserWithEmailAndPassword(email.trim(), password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                long now = System.currentTimeMillis();
                UserProfile profile = new UserProfile(
                        user.getUid(),
                        name.trim(),
                        user.getEmail(),
                        user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                        activeMode != null ? activeMode : UserProfile.MODE_TRAVELER,
                        now,
                        now
                );

                userRepository.createUserProfile(profile, new UserRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        userProfileLiveData.setValue(Resource.success(profile));
                        sendEmailVerification();
                        authStateLiveData.setValue(Resource.success(user));
                    }

                    @Override
                    public void onError(Exception exception) {
                        userProfileLiveData.setValue(Resource.error("Profile creation warning: " + exception.getLocalizedMessage(), profile));
                        authStateLiveData.setValue(Resource.success(user));
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                authStateLiveData.setValue(Resource.error(exception.getLocalizedMessage(), null));
            }
        });
    }

    public void signInWithGoogleCredential(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            operationStatusLiveData.setValue(Resource.error("Google Sign-In Token missing.", null));
            return;
        }

        authStateLiveData.setValue(Resource.loading(null));
        var credential = GoogleAuthProvider.getCredential(idToken, null);
        authRepository.signInWithCredential(credential, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // Ensure profile exists in Firestore
                userRepository.getUserProfile(user.getUid(), new UserRepository.ProfileCallback() {
                    @Override
                    public void onSuccess(UserProfile profile) {
                        if (profile == null) {
                            long now = System.currentTimeMillis();
                            UserProfile newProfile = new UserProfile(
                                    user.getUid(),
                                    user.getDisplayName() != null ? user.getDisplayName() : "Google User",
                                    user.getEmail(),
                                    user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                                    UserProfile.MODE_TRAVELER,
                                    now,
                                    now
                            );
                            userRepository.createUserProfile(newProfile, new UserRepository.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    userProfileLiveData.setValue(Resource.success(newProfile));
                                }

                                @Override
                                public void onError(Exception exception) {
                                    userProfileLiveData.setValue(Resource.success(newProfile));
                                }
                            });
                        } else {
                            userProfileLiveData.setValue(Resource.success(profile));
                        }
                        authStateLiveData.setValue(Resource.success(user));
                    }

                    @Override
                    public void onError(Exception exception) {
                        authStateLiveData.setValue(Resource.success(user));
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                authStateLiveData.setValue(Resource.error(exception.getLocalizedMessage(), null));
            }
        });
    }

    public void sendEmailVerification() {
        operationStatusLiveData.setValue(Resource.loading("Sending verification email..."));
        authRepository.sendEmailVerification(new AuthRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                operationStatusLiveData.setValue(Resource.success("Verification email sent! Please check your inbox."));
            }

            @Override
            public void onError(Exception exception) {
                operationStatusLiveData.setValue(Resource.error("Failed to send verification email: " + exception.getLocalizedMessage(), null));
            }
        });
    }

    public void checkEmailVerification() {
        operationStatusLiveData.setValue(Resource.loading("Checking verification status..."));
        authRepository.reloadUser(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (user != null && user.isEmailVerified()) {
                    operationStatusLiveData.setValue(Resource.success("Email verified successfully!"));
                    authStateLiveData.setValue(Resource.success(user));
                } else {
                    operationStatusLiveData.setValue(Resource.error("Email is not verified yet. Please check your link.", null));
                }
            }

            @Override
            public void onError(Exception exception) {
                operationStatusLiveData.setValue(Resource.error("Unable to refresh verification status: " + exception.getLocalizedMessage(), null));
            }
        });
    }

    public void sendPasswordReset(String email) {
        if (email == null || email.trim().isEmpty()) {
            operationStatusLiveData.setValue(Resource.error("Please enter your registered email address.", null));
            return;
        }

        operationStatusLiveData.setValue(Resource.loading("Sending reset link..."));
        authRepository.sendPasswordResetEmail(email.trim(), new AuthRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                operationStatusLiveData.setValue(Resource.success("Password reset link sent to " + email.trim()));
            }

            @Override
            public void onError(Exception exception) {
                operationStatusLiveData.setValue(Resource.error("Failed to send reset link: " + exception.getLocalizedMessage(), null));
            }
        });
    }

    public void loadProfile(String uid) {
        if (uid == null) return;
        userProfileLiveData.setValue(Resource.loading(null));
        userRepository.getUserProfile(uid, new UserRepository.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                userProfileLiveData.setValue(Resource.success(profile));
            }

            @Override
            public void onError(Exception exception) {
                userProfileLiveData.setValue(Resource.error(exception.getLocalizedMessage(), null));
            }
        });
    }

    public void updateActiveMode(String activeMode) {
        FirebaseUser user = getCurrentUser();
        if (user == null) return;

        userRepository.updateActiveMode(user.getUid(), activeMode, new UserRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                loadProfile(user.getUid());
                operationStatusLiveData.setValue(Resource.success("Role mode updated to " + activeMode));
            }

            @Override
            public void onError(Exception exception) {
                operationStatusLiveData.setValue(Resource.error("Failed to update role mode: " + exception.getLocalizedMessage(), null));
            }
        });
    }

    public void signOut() {
        authRepository.signOut();
        authStateLiveData.setValue(null);
        userProfileLiveData.setValue(null);
    }
}