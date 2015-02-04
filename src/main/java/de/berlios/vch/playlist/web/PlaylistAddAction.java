package de.berlios.vch.playlist.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.playlist.PlaylistService;
import de.berlios.vch.web.IWebAction;

@Component
@Provides
public class PlaylistAddAction implements IWebAction {

    @Requires(filter = "(instance.name=vch.web.playlist)")
    private ResourceBundleProvider i18n;

    /*
     * this web action can only work, if the playlist service is available, though it does not depend on it directly. if
     * the playlist service is unavailable, the playlist servlet is invalid / unavailable and this action gets an 404.
     * that's why we have this requirement
     */
    @Requires
    private PlaylistService playlistService;

    @Override
    public String getUri(IWebPage page) throws UnsupportedEncodingException {
        IVideoPage video = (IVideoPage) page;
        String uri = URLEncoder.encode(video.getVchUri().toString(), "UTF-8");
        return PlaylistServlet.PATH + "?action=add&uri=" + uri;
    }

    @Override
    public String getTitle() {
        return i18n.getResourceBundle().getString("I18N_ADD_TO_PLAYLIST");
    }

}
