package de.htw.ar.treasurehuntar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Activity
 */
public class MainActivityGoogleGlass extends Activity {

    public static final String EXTRAS_KEY_ACTIVITY_TITLE_STRING = "activityTitle";
    public static final String EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL = "activityArchitectWorldUrl";

    private CardScrollView mCardScrollView;
    private SimpleCardScrollAdapter mScrollAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCardScrollView = new CardScrollView(this);

        // Create two card
        // 1. discover with DiscoverActivity
        // 2. caching with CachingActivity and
        List<CardMeta> cards = new ArrayList<>();
        cards.add(new CardMeta(this, "Discover", DiscoverActivity.class));
        cards.add(new CardMeta(this, "Caching", CachingActivity.class));

        // Scroll adapter with the two cards
        mScrollAdapter = new SimpleCardScrollAdapter(cards);

        mCardScrollView.setAdapter(mScrollAdapter);
        mCardScrollView.setOnItemClickListener(mScrollAdapter);
        mCardScrollView.activate();

        // set scroll adapter as view
        setContentView(mCardScrollView);
    }

    /**
     * helper to check if video-drawables are supported by this device.
     * recommended to check before launching ARchitect Worlds with
     * videodrawables
     *
     * @return true if AR.VideoDrawables are supported, false if fallback
     * rendering would apply (= show video fullscreen)
     */
    public static final boolean isVideoDrawablesSupported() {
        String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        return extensions != null
            && extensions.contains("GL_OES_EGL_image_external")
            && android.os.Build.VERSION.SDK_INT >= 14;
    }

    public static int getStringIdentifier(final Context context,
        final String name) {
        return context.getResources()
            .getIdentifier(name.toLowerCase(), "string",
                context.getPackageName());
    }

    private class SimpleCardScrollAdapter extends CardScrollAdapter implements
        AdapterView.OnItemClickListener {

        final private List<CardMeta> mCards;

        /**
         * Adding to cards to scroller
         *
         * @param cards
         */
        public SimpleCardScrollAdapter(List<CardMeta> cards) {
            if (cards == null || cards.size() == 0) {
                throw new IllegalArgumentException("No cards provided");
            }

            mCards = cards;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPosition(Object item) {
            if (item instanceof CardMeta) {
                return mCards.indexOf(item);
            }

            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount() {
            return mCards.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getItemViewType(int position) {
            return mCards.get(position).cardBuilder.getItemViewType();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).cardBuilder
                .getView(convertView, parent);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onItemClick(AdapterView<?> parent, View view,
            int position, long id) {

            // get className of activity to call when clicking item at position x
            final CardMeta cardMeta = getClickedCard(position);

            try {
                final Intent intent = new Intent(
                    MainActivityGoogleGlass.this,
                    cardMeta.activityClass);

                intent.putExtra(EXTRAS_KEY_ACTIVITY_TITLE_STRING,
                    cardMeta.cardName);
                intent.putExtra(EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL,
                    cardMeta.pathToARchitect);

                if (!MainActivityGoogleGlass
                    .isVideoDrawablesSupported()) {
                    Toast.makeText(MainActivityGoogleGlass.this,
                        R.string.videosrawables_fallback,
                        Toast.LENGTH_LONG).show();
                }

                // start activity
                MainActivityGoogleGlass.this
                    .startActivity(intent);

            } catch (Exception e) {
                // may never occur, as long as all SampleActivities exist and are
                // listed in manifest
                Toast.makeText(MainActivityGoogleGlass.this,
                    cardMeta.activityClass.getName()
                        + "\nnot defined/accessible",
                    Toast.LENGTH_SHORT).show();
                Log.e("onItemClick", e.getMessage());
            }
        }

        private CardMeta getClickedCard(final int position) {
            return (CardMeta) getItem(position);
        }
    }

    /**
     * Holds the card, and activity to start
     */
    private static class CardMeta {

        final String pathToARchitect;
        final CardBuilder cardBuilder;
        final String cardName;
        final Class activityClass;

        public CardMeta(Context context, String cardName, Class activityClass) {
            this.cardName = cardName;
            this.pathToARchitect = cardName + ".html";
            this.activityClass = activityClass;

            Log.i("cardMeta", "cardName: " + cardName + "; path to architect: "
                + pathToARchitect);

            cardBuilder = new CardBuilder(context, CardBuilder.Layout.TEXT)
                .setText(getStringIdentifier(context,
                    cardName + "_title"));
        }

        @Override
        public String toString() {
            return "activityClass:" + this.activityClass.getName()
                + ", cardName: " + this.cardName + ", pathToARchitect: "
                + this.pathToARchitect;
        }
    }
}
