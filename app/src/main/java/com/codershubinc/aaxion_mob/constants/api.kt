package com.codershubinc.aaxion_mob.constants

object API_ENDPOINTS {
    // ==========================================
    // Authentication
    // ==========================================
    object AUTH {
        const val LOGIN = "/api/v1/auth/login"
        const val REGISTER = "/api/v1/auth/register"
        const val LOGOUT = "/api/v1/auth/logout"
        const val TOKEN_GENERATE = "/api/v1/auth/token/generate"
        const val TOKEN_REMOVE = "/api/v1/auth/token/remove"
    }

    // ==========================================
    // WebDAV
    // ==========================================
    object WEBDAV {
        const val ROOT = "/webdav/"
    }

    // ==========================================
    // Files & Directories
    // ==========================================
    object FILES {
        const val VIEW = "/api/v1/files/view"
        const val CREATE_DIR = "/api/v1/files/directory/create"
        const val UNZIP = "/api/v1/files/unzip"
        const val UPLOAD = "/api/v1/files/upload"
        const val DOWNLOAD = "/api/v1/files/download"
        const val STREAM = "/api/v1/files/stream"
        const val THUMBNAIL = "/api/v1/images/thumbnail"
        const val VIEW_IMAGE = "/api/v1/images/view"
        const val SHARE_REQUEST = "/api/v1/share/temp/request"
        const val SHARE_DOWNLOAD = "/api/v1/share/temp" // /api/v1/share/temp/{token}
    }

    // ==========================================
    // Images
    // ==========================================
    object IMAGES {
        const val THUMBNAIL = "/api/v1/images/thumbnail"
        const val THUMB = "/api/v1/images/thumb"
        const val VIEW = "/api/v1/images/view"
    }

    // ==========================================
    // Temporary File Sharing
    // ==========================================
    object SHARE {
        const val TEMP = "/api/v1/share/temp" // /api/v1/share/temp/{token}
        const val REQUEST = "/api/v1/share/temp/request"
    }

    // ==========================================
    // Anonymous & Token-based Uploads
    // ==========================================
    object TOKENS {
        const val GENERATE = "/api/v1/anonymous/token/generate"
        const val REVOKE = "/api/v1/anonymous/token/revoke"
        const val LIST = "/api/v1/anonymous/tokens"
        const val INFO = "/api/v1/anonymous/token/info"
        const val VALIDATE = "/api/v1/anonymous/token/validate"
        const val GUEST_UPLOAD_FILE = "/api/v1/anonymous/upload"
        const val GUEST_UPLOAD_CHUNK_START = "/api/v1/anonymous/upload/chunk/start"
        const val GUEST_UPLOAD_CHUNK = "/api/v1/anonymous/upload/chunk"
        const val GUEST_UPLOAD_CHUNK_COMPLETE = "/api/v1/anonymous/upload/chunk/complete"
    }

    // ==========================================
    // File Chunked Upload
    // ==========================================
    object CHUNKS {
        const val START = "/api/v1/files/upload/chunk/start"
        const val UPLOAD = "/api/v1/files/upload/chunk"
        const val COMPLETE = "/api/v1/files/upload/chunk/complete"
    }

    // ==========================================
    // System Information
    // ==========================================
    object SYSTEM {
        const val INFO = "/api/v1/system/info"
        const val GET_ROOT_PATH = "/api/v1/system/root-path"
        const val STORAGE = "/api/v1/system/storage"
    }

    // ==========================================
    // Movies
    // ==========================================
    object MOVIES {
        const val LIST = "/api/v1/movies"
        const val SEARCH = "/api/v1/movies/search"
        const val ADD = "/api/v1/movies/add"
        const val EDIT = "/api/v1/movies/edit"
        const val STREAM = "/api/v1/movies/stream"
    }

    // ==========================================
    // Series & Episodes
    // ==========================================
    object SERIES {
        const val LIST = "/api/v1/series"
        const val SEARCH = "/api/v1/series/search"
        const val ADD = "/api/v1/series/add"
        const val EDIT = "/api/v1/series/edit"
        const val EPISODES = "/api/v1/series/episodes"
        const val EPISODE_ADD = "/api/v1/series/episodes/add"
        const val EPISODE_STREAM = "/api/v1/series/episodes/stream"
    }

    // ==========================================
    // Music
    // ==========================================
    object MUSIC {
        const val TRACKS = "/api/v1/music"
        const val GET = "/api/v1/music/get"
        const val SEARCH = "/api/v1/music/search"
        const val ADD = "/api/v1/music/add"
        const val UPDATE = "/api/v1/music/update"
        const val STREAM = "/api/v1/music/stream"

        object STATS {
            const val PLAY = "/api/v1/music/stats/play"
            const val FAVORITE = "/api/v1/music/stats/favorite"
            const val PLAY_STATE = "/api/v1/music/stats/play-state"
            const val PLAY_STATES = "/api/v1/music/stats/play-states"
            const val IS_FAVORITE = "/api/v1/music/stats/is-favorite"
            const val LAST_PLAYED = "/api/v1/music/stats/last-played"
        }
    }

    // ==========================================
    // WebSockets & Devices
    // ==========================================
    object DEVICES {
        const val LIST = "/api/v1/devices"
        const val WS = "/api/v1/ws"
    }
}