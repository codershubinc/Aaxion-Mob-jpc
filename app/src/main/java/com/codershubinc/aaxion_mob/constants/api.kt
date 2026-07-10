package com.codershubinc.aaxion_mob.constants

object API_ENDPOINTS {
    object AUTH {
        const val LOGIN = "/auth/login"
        const val REGISTER = "/auth/register"
        const val LOGOUT = "/auth/logout"
    }
    object FILES {
        const val VIEW = "/api/files/view"
        const val CREATE_DIR = "/files/create-directory"
        const val UPLOAD = "/files/upload"
        const val DOWNLOAD = "/files/download"
        const val THUMBNAIL = "/files/thumbnail"
        const val VIEW_IMAGE = "/files/view-image"
        const val SHARE_REQUEST = "/files/d/r"
        const val SHARE_DOWNLOAD = "/files/d/t" // /files/d/t/{token}
    }
    object TOKENS {
        const val GENERATE = "/api/upload-tokens/generate"
        const val REVOKE = "/api/upload-tokens/revoke"
        const val LIST = "/api/upload-tokens/list"
        const val INFO = "/api/upload-tokens/info"
        const val VALIDATE = "/upload/token/validate"
        const val GUEST_UPLOAD_FILE = "/upload/token/file"
        const val GUEST_UPLOAD_CHUNK_START = "/upload/token/chunk/start"
        const val GUEST_UPLOAD_CHUNK = "/upload/token/chunk"
        const val GUEST_UPLOAD_CHUNK_COMPLETE = "/upload/token/chunk/complete"
    }
    object CHUNKS {
        const val START = "/files/upload/chunk/start"
        const val UPLOAD = "/files/upload/chunk"
        const val COMPLETE = "/files/upload/chunk/complete"
    }
    object SYSTEM {
        const val GET_ROOT_PATH = "/api/system/get-root-path"
        const val STORAGE = "/api/system/storage"
    }
}