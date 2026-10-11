package lk.falconplan.app.data.repository;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepositoryImpl implements AuthRepository {

    private final FirebaseAuth firebaseAuth;

    public AuthRepositoryImpl() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public AuthRepositoryImpl(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    @Override
    public boolean isSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Override
    public boolean isEmailVerified() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    @Override
    public void signOut() {
        firebaseAuth.signOut();
    }

    @Override
    public void signInWithEmailAndPassword(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (callback != null) callback.onSuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void createUserWithEmailAndPassword(String email, String password, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (callback != null) callback.onSuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void sendPasswordResetEmail(String email, OperationCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void sendEmailVerification(OperationCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError(new IllegalStateException("No user is signed in."));
            return;
        }

        user.sendEmailVerification()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void reloadUser(AuthCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError(new IllegalStateException("No user is signed in."));
            return;
        }

        user.reload()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(firebaseAuth.getCurrentUser());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void signInWithCredential(AuthCredential credential, AuthCallback callback) {
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    if (callback != null) callback.onSuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }
}