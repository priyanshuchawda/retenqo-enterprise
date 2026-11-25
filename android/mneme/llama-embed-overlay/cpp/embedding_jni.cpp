#include <android/log.h>
#include <jni.h>

#include <string>
#include <unistd.h>
#include <vector>

#include "common.h"
#include "llama.h"
#include "logging.h"

/**
 * Separate embedding model instance (does not share g_model from ai_chat.cpp).
 * Call unload before loading the chat LLM on 6 GB phones.
 */
static llama_model *   g_embed_model = nullptr;
static llama_context * g_embed_ctx   = nullptr;
static bool            g_embed_backend_ready = false;

static void ensure_backend(JNIEnv * env, jstring jnative_lib_dir) {
    if (g_embed_backend_ready) return;
    const char * path = env->GetStringUTFChars(jnative_lib_dir, nullptr);
    LOGi("Embedding: loading backends from %s", path);
    ggml_backend_load_all_from_path(path);
    env->ReleaseStringUTFChars(jnative_lib_dir, path);
    llama_backend_init();
    g_embed_backend_ready = true;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_arm_aichat_EmbeddingEngine_nativeLoad(
        JNIEnv * env,
        jobject /*unused*/,
        jstring jnative_lib_dir,
        jstring jmodel_path) {
    ensure_backend(env, jnative_lib_dir);

    if (g_embed_ctx) {
        llama_free(g_embed_ctx);
        g_embed_ctx = nullptr;
    }
    if (g_embed_model) {
        llama_model_free(g_embed_model);
        g_embed_model = nullptr;
    }

    const char * model_path = env->GetStringUTFChars(jmodel_path, nullptr);
    LOGi("Embedding: loading %s", model_path);

    llama_model_params model_params = llama_model_default_params();
    g_embed_model = llama_model_load_from_file(model_path, model_params);
    env->ReleaseStringUTFChars(jmodel_path, model_path);
    if (!g_embed_model) {
        LOGe("Embedding: model load failed");
        return 1;
    }

    const int n_threads = std::max(2, std::min(4, (int) sysconf(_SC_NPROCESSORS_ONLN) - 2));
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 512;
    ctx_params.n_batch = 512;
    ctx_params.n_ubatch = 512;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;
    ctx_params.embeddings = true;
    ctx_params.pooling_type = LLAMA_POOLING_TYPE_UNSPECIFIED; // use model default

    g_embed_ctx = llama_init_from_model(g_embed_model, ctx_params);
    if (!g_embed_ctx) {
        LOGe("Embedding: context init failed");
        llama_model_free(g_embed_model);
        g_embed_model = nullptr;
        return 2;
    }
    llama_set_embeddings(g_embed_ctx, true);
    LOGi("Embedding: ready (n_embd_out=%d pooling=%d)",
         llama_model_n_embd_out(g_embed_model),
         (int) llama_pooling_type(g_embed_ctx));
    return 0;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_arm_aichat_EmbeddingEngine_nativeEmbed(JNIEnv * env, jobject /*unused*/, jstring jtext) {
    if (!g_embed_ctx || !g_embed_model) {
        LOGe("Embedding: not loaded");
        return nullptr;
    }

    const char * text = env->GetStringUTFChars(jtext, nullptr);
    std::string prompt(text ? text : "");
    env->ReleaseStringUTFChars(jtext, text);
    if (prompt.empty()) {
        return nullptr;
    }

    auto tokens = common_tokenize(g_embed_ctx, prompt, true, true);
    if (tokens.empty()) {
        LOGe("Embedding: tokenize empty");
        return nullptr;
    }
    if ((int) tokens.size() > 512) {
        tokens.resize(512);
    }

    llama_batch batch = llama_batch_init((int32_t) tokens.size(), 0, 1);
    for (int i = 0; i < (int) tokens.size(); i++) {
        batch.token[i] = tokens[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = true;
        ++batch.n_tokens;
    }

    llama_memory_clear(llama_get_memory(g_embed_ctx), true);
    if (llama_decode(g_embed_ctx, batch) < 0) {
        LOGe("Embedding: decode failed");
        llama_batch_free(batch);
        return nullptr;
    }

    const int n_embd = llama_model_n_embd_out(g_embed_model);
    const enum llama_pooling_type pooling = llama_pooling_type(g_embed_ctx);
    const float * embd = nullptr;
    if (pooling == LLAMA_POOLING_TYPE_NONE) {
        embd = llama_get_embeddings_ith(g_embed_ctx, -1);
    } else {
        embd = llama_get_embeddings_seq(g_embed_ctx, 0);
    }
    llama_batch_free(batch);

    if (!embd) {
        LOGe("Embedding: null embedding pointer");
        return nullptr;
    }

    std::vector<float> out(n_embd);
    common_embd_normalize(embd, out.data(), n_embd, /*euclid*/ 2);

    jfloatArray arr = env->NewFloatArray(n_embd);
    if (!arr) return nullptr;
    env->SetFloatArrayRegion(arr, 0, n_embd, out.data());
    return arr;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_arm_aichat_EmbeddingEngine_nativeUnload(JNIEnv *, jobject /*unused*/) {
    if (g_embed_ctx) {
        llama_free(g_embed_ctx);
        g_embed_ctx = nullptr;
    }
    if (g_embed_model) {
        llama_model_free(g_embed_model);
        g_embed_model = nullptr;
    }
    LOGi("Embedding: unloaded");
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_arm_aichat_EmbeddingEngine_nativeIsLoaded(JNIEnv *, jobject /*unused*/) {
    return g_embed_model != nullptr && g_embed_ctx != nullptr ? JNI_TRUE : JNI_FALSE;
}
