package lk.falconplan.app.data.repository;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.data.model.CommunityPost;
import lk.falconplan.app.data.model.HomestayListing;

public class MockDataRepository {

    private static MockDataRepository instance;

    private MockDataRepository() {}

    public static synchronized MockDataRepository getInstance() {
        if (instance == null) {
            instance = new MockDataRepository();
        }
        return instance;
    }

    public List<HomestayListing> getFeaturedHomestays() {
        List<HomestayListing> list = new ArrayList<>();
        list.add(new HomestayListing(
                "preview_01",
                "Meemure Heritage Eco Lodge",
                "Meemure Village, Knuckles Range",
                "Knuckles Villages",
                8500.00,
                4.9f,
                28,
                "Bandara & Family",
                "Local Host • 3rd Gen Villager",
                "Experience traditional clay-house living nestled beneath Lakegala peak. Includes home-cooked organic meals, river bathing guidance, and village storytelling.",
                true
        ));

        list.add(new HomestayListing(
                "preview_02",
                "Ella Gap Spice Garden Cottage",
                "Ella, Badulla District",
                "Ella & Hill Country",
                10500.00,
                4.8f,
                42,
                "Samanthi Perera",
                "Local Host • Spice Farming Family",
                "Scenic wooden cottage surrounded by cardamoms and tea bushes. Enjoy morning misty views, cooking lessons, and hiking trails to Ella Rock.",
                true
        ));

        list.add(new HomestayListing(
                "preview_03",
                "Sigiriya Village Farm Stay",
                "Inamaluwa, Cultural Triangle",
                "Cultural Triangle",
                7200.00,
                4.7f,
                19,
                "Nimal & Champa",
                "Local Host • Paddy Farmer",
                "Authentic village bungalow close to Sigiriya Lion Rock. Try traditional chena farming, bullock cart rides, and fresh lotus-leaf lunches.",
                true
        ));

        list.add(new HomestayListing(
                "preview_04",
                "Sinharaja Canopy Edge Lodge",
                "Kudawa, Sinharaja Border",
                "Sinharaja Edge",
                9800.00,
                4.9f,
                31,
                "Gamini Herath",
                "Local Host • Certified Eco Guide",
                "Eco-conscious wooden retreat on the rainforest border. Guided birdwatching, endemic plant walks, and natural spring baths.",
                true
        ));

        return list;
    }

    public List<CommunityPost> getCommunityPosts() {
        List<CommunityPost> posts = new ArrayList<>();
        posts.add(new CommunityPost(
                "post_01",
                "Champa Ranasinghe",
                "Host • Sigiriya",
                "Inamaluwa Village",
                "2 hours ago",
                "Preparing Traditional Clay Pot Cooking Workshop",
                "Today our community host collective gathered to share recipes for Sri Lankan heirloom rice and jackfruit curry. Travelers are always welcome to join us in the kitchen!",
                24,
                8,
                true
        ));

        posts.add(new CommunityPost(
                "post_02",
                "Bandara S.",
                "Community Lead • Knuckles",
                "Meemure",
                "Yesterday",
                "Community Trail Clean-Up & Tree Planting",
                "Village youth and travelers united this weekend to plant 50 native trees along the Lakegala hiking trail. Protecting our ecosystem together!",
                45,
                14,
                true
        ));

        posts.add(new CommunityPost(
                "post_03",
                "Kavinda Dissanayake",
                "Traveler",
                "Ella & Bandarawela",
                "3 days ago",
                "Reflections on my 3-day stay in Ella Spice Cottage",
                "Warmest hospitality I have ever experienced. Drinking morning Ceylon tea while watching the mist rise over the mountains is something I will never forget.",
                38,
                11,
                true
        ));

        return posts;
    }
}