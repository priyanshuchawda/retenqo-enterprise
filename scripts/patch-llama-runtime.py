#!/usr/bin/env python3
"""Apply Retenqo's bounded, privacy-aware additions to the pinned llama.cpp Android API."""
from pathlib import Path
import sys

lib = Path(sys.argv[1])


def replace_once(relative_path: str, old: str, new: str) -> None:
    path = lib / relative_path
    source = path.read_text()
    if old in source:
        path.write_text(source.replace(old, new, 1))
    elif new in source:
        return
    else:
        raise SystemExit(f"Cannot patch pinned llama.cpp source: {path}")


def insert_before_once(relative_path: str, marker: str, addition: str, signature: str) -> None:
    path = lib / relative_path
    source = path.read_text()
    if signature in source:
        return
    if marker not in source:
        raise SystemExit(f"Cannot patch pinned llama.cpp source: {path}")
    path.write_text(source.replace(marker, addition + marker, 1))


insert_before_once(
    "src/main/java/com/arm/aichat/InferenceEngine.kt",
    "    /**\n     * Sends a user prompt to the loaded model and returns a Flow of generated tokens.\n     */\n",
    "    /** Reset chat history and KV cache for a new independent request. */\n"
    "    suspend fun resetConversation(systemPrompt: String)\n\n",
    "suspend fun resetConversation(systemPrompt: String)",
)

insert_before_once(
    "src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt",
    "    /**\n     * Send plain text user prompt to LLM, which starts generating tokens in a [Flow]\n",
    "    /** Reset conversation state and install a fresh system prompt before each request. */\n"
    "    override suspend fun resetConversation(systemPrompt: String) =\n"
    "        withContext(llamaDispatcher) {\n"
    "            require(systemPrompt.isNotBlank()) { \"Cannot reset to an empty system prompt!\" }\n"
    "            check(_state.value is InferenceEngine.State.ModelReady) {\n"
    "                \"Cannot reset conversation in ${_state.value.javaClass.simpleName}\"\n"
    "            }\n"
    "            _readyForSystemPrompt = false\n"
    "            _state.value = InferenceEngine.State.ProcessingSystemPrompt\n"
    "            val result = processSystemPrompt(systemPrompt)\n"
    "            if (result != 0) {\n"
    "                val error = RuntimeException(\"Failed to reset conversation: $result\")\n"
    "                _state.value = InferenceEngine.State.Error(error)\n"
    "                throw error\n"
    "            }\n"
    "            _state.value = InferenceEngine.State.ModelReady\n"
    "        }\n\n",
    "override suspend fun resetConversation(systemPrompt: String)",
)

# Repair sources affected by the earlier duplicated KDoc marker.
replace_once(
    "src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt",
    "    /**\n     * Send plain text user prompt to LLM, which starts generating tokens in a [Flow]\n"
    "    /**\n     * Send plain text user prompt to LLM, which starts generating tokens in a [Flow]\n",
    "    /**\n     * Send plain text user prompt to LLM, which starts generating tokens in a [Flow]\n",
)

replace_once(
    "src/main/cpp/ai_chat.cpp",
    '    LOGd("%s: User prompt received: \\n%s", __func__, user_prompt);',
    '    LOGd("%s: User prompt received.", __func__);',
)
replace_once(
    "src/main/cpp/ai_chat.cpp",
    '    LOGd("%s: System prompt received: \\n%s", __func__, system_prompt);',
    '    LOGd("%s: System prompt received.", __func__);',
)
replace_once(
    "src/main/cpp/ai_chat.cpp",
    '    for (auto id: system_tokens) {\n'
    '        LOGv("token: `%s`\\t -> `%d`", common_token_to_piece(g_context, id).c_str(), id);\n'
    '    }',
    '    // Prompt token pieces can contain private user data; never log them.',
)
replace_once(
    "src/main/cpp/ai_chat.cpp",
    '    for (auto id: user_tokens) {\n'
    '        LOGv("token: `%s`\\t -> `%d`", common_token_to_piece(g_context, id).c_str(), id);\n'
    '    }',
    '    // Prompt token pieces can contain private user data; never log them.',
)
replace_once(
    "src/main/cpp/ai_chat.cpp",
    '        LOGv("id: %d,\\tcached: `%s`,\\tnew: `%s`", new_token_id, cached_token_chars.c_str(), new_token_chars.c_str());',
    '        // Generated text is private; do not write it to logcat.',
)
