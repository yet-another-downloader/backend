package com.nbakaev.yad.gateway.youtube;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * {
 *  "kind": "youtube#videoListResponse",
 *  "etag": "\"p4VTdlkQv3HQeTEaXgvLePAydmU/yI_RQ0y6FdoWO-LGE0tUvT1kAK4\"",
 *  "pageInfo": {
 *   "totalResults": 1,
 *   "resultsPerPage": 1
 *  },
 *  "items": [
 *   {
 *    "kind": "youtube#video",
 *    "etag": "\"p4VTdlkQv3HQeTEaXgvLePAydmU/ujhTQ2zGL2snT5bixDDPvEk_eZU\"",
 *    "id": "IvK0ZlNoxjw",
 *    "snippet": {
 *     "publishedAt": "2018-04-23T16:25:02.000Z",
 *     "channelId": "UC0JB7TSe49lg56u6qH8y_MQ",
 *     "title": "Nuts and Bolts: Modular AI From the Ground Up",
 *     "description": "In this 2016 GDC panel, programmers Kevin Dill, Christopher Dragert & Troy Humphreys provide a comprehensive exploration of modular AI, from the theoretical underpinnings up to code examples from shipped titles.\n\n\nRegister for GDC: http://ubm.io/2gk5KTU\n\nJoin the GDC mailing list: http://www.gdconf.com/subscribe\n\nFollow GDC on Twitter: https://twitter.com/Official_GDC \n\nGDC talks cover a range of developmental topics including game design, programming, audio, visual arts, business management, production, online games, and much more. We post a fresh GDC video every day.  Subscribe to the channel to stay on top of regular updates, and check out GDC Vault  for thousands of more in-depth talks from our archives.",
 *     "thumbnails": {
 *      "default": {
 *       "url": "https://i.ytimg.com/vi/IvK0ZlNoxjw/default.jpg",
 *       "width": 120,
 *       "height": 90
 *      },
 *      "medium": {
 *       "url": "https://i.ytimg.com/vi/IvK0ZlNoxjw/mqdefault.jpg",
 *       "width": 320,
 *       "height": 180
 *      },
 *      "high": {
 *       "url": "https://i.ytimg.com/vi/IvK0ZlNoxjw/hqdefault.jpg",
 *       "width": 480,
 *       "height": 360
 *      },
 *      "standard": {
 *       "url": "https://i.ytimg.com/vi/IvK0ZlNoxjw/sddefault.jpg",
 *       "width": 640,
 *       "height": 480
 *      },
 *      "maxres": {
 *       "url": "https://i.ytimg.com/vi/IvK0ZlNoxjw/maxresdefault.jpg",
 *       "width": 1280,
 *       "height": 720
 *      }
 *     },
 *     "channelTitle": "GDC",
 *     "tags": [
 *      "gdc",
 *      "talk",
 *      "panel",
 *      "game",
 *      "games",
 *      "gaming",
 *      "development",
 *      "hd",
 *      "design",
 *      "programming",
 *      "game programming",
 *      "AI",
 *      "artificial intelligence"
 *     ],
 *     "categoryId": "27",
 *     "liveBroadcastContent": "none",
 *     "defaultLanguage": "en",
 *     "localized": {
 *      "title": "Nuts and Bolts: Modular AI From the Ground Up",
 *      "description": "In this 2016 GDC panel, programmers Kevin Dill, Christopher Dragert & Troy Humphreys provide a comprehensive exploration of modular AI, from the theoretical underpinnings up to code examples from shipped titles.\n\n\nRegister for GDC: http://ubm.io/2gk5KTU\n\nJoin the GDC mailing list: http://www.gdconf.com/subscribe\n\nFollow GDC on Twitter: https://twitter.com/Official_GDC \n\nGDC talks cover a range of developmental topics including game design, programming, audio, visual arts, business management, production, online games, and much more. We post a fresh GDC video every day.  Subscribe to the channel to stay on top of regular updates, and check out GDC Vault  for thousands of more in-depth talks from our archives."
 *     },
 *     "defaultAudioLanguage": "en"
 *    }
 *   }
 *  ]
 * }
 */
@Data
public class YoutubeVideoItemApi3Dto {

    // youtube#video
    private String kind;

    private List<Item> items;

    private PageInfo pageInfo;

    @Data
    public static class Item {

        private Snippet snippet;

        @Data
        public static class Snippet {
            private String title;

            private Map<String, ThumbnailItem> thumbnails;

            @Data
            public static class ThumbnailItem {
                private String url;
            }

        }

    }

    @Data
    public static class PageInfo {
        // 1
        private long totalResults;

        // 1
        private long resultsPerPage;
    }

}
