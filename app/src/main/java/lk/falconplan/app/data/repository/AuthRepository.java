package lk.falconplan.app.data.repository;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;

public interface AuthRepository {

    FirebaseUser getCurrentUser();

    boolean isSignedIn();

    boolean isEmailVerified();

    void signOut();

    void signInWithEmailAndPassword(String email, String password, AuthCallback callback);

    void createUserWithEmailAndPassword(String email, String password, AuthCallback callback);

    void sendPasswordResetEmail(String email, OperationCallback callback);

    void sendEmailVerification(OperationCallback callback);

    void reloadUser(AuthCallback callback);

    void signInWithCredential(AuthCredential credential, AuthCallback callback);

    interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }
}