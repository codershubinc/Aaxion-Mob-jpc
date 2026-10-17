package com.codershubinc.aaxion_mob.core.network

object ApiEndpoints {
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
    // Music
    // ==========================================
    object MUSIC {
        const val LIST = "/api/v1/music"
        const val GET = "/api/v1/music/get"
        const val SEARCH = "/api/v1/music/search"
        const val ADD = "/api/v1/music/add"
        const val UPDATE = "/api/v1/music/update"
        const val STREAM = "/api/v1/music/stream"
    }
}

typealias API_ENDPOINTS = ApiEndpoints
