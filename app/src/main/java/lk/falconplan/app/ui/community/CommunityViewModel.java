package lk.falconplan.app.ui.community;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import lk.falconplan.app.data.model.CommunityPost;
import lk.falconplan.app.data.repository.MockDataRepository;

public class CommunityViewModel extends ViewModel {

    private final MutableLiveData<List<CommunityPost>> postsLiveData = new MutableLiveData<>();
    private final MockDataRepository repository;

    public CommunityViewModel() {
        this.repository = MockDataRepository.getInstance();
        loadPosts();
    }

    public LiveData<List<CommunityPost>> getPostsLiveData() {
        return postsLiveData;
    }

    public void loadPosts() {
        postsLiveData.setValue(repository.getCommunityPosts());
    }
}