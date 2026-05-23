package com.aistudio.shortsgen.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import android.util.Log
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class AutoPublisherService : AccessibilityService() {
    
    companion object {
        const val ACTION_START_AUTOMATION = "com.aistudio.shortsgen.START_AUTOMATION"
        const val EXTRA_PROMPT = "extra_prompt"
        const val EXTRA_CAPTION = "extra_caption"
        private const val TAG = "AutoPublisherService"
    }

    private var currentState = State.IDLE
    private var prompt = ""
    private var caption = ""
    private val handler = Handler(Looper.getMainLooper())
    private var baselineLastModified: Long = -1L
    private var downloadedVideoUri: Uri? = null

    enum class State {
        IDLE,
        GEMINI_FIND_INPUT,
        GEMINI_WAIT_GEN,
        WAITING_FOR_DOWNLOAD_FILE,
        INSTA_UPLOAD_UI,
        YOUTUBE_UPLOAD_UI
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_START_AUTOMATION) {
                prompt = intent.getStringExtra(EXTRA_PROMPT) ?: ""
                caption = intent.getStringExtra(EXTRA_CAPTION) ?: ""
                currentState = State.GEMINI_FIND_INPUT
                baselineLastModified = getLatestDownloadTime() // Record the state of the downloads folder
                Toast.makeText(context, "Bot Started: Monitoring downloads...", Toast.LENGTH_LONG).show()
                
                Log.d(TAG, "Starting automation for prompt: \$prompt")
                
                // Launch Gemini App directly
                val geminiIntent = packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
                if (geminiIntent != null) {
                    geminiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(geminiIntent)
                } else {
                    Toast.makeText(context, "Error: Gemini App is not installed!", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Gemini app not found. Automation aborted.")
                    currentState = State.IDLE
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected")
        val filter = IntentFilter(ACTION_START_AUTOMATION)
        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private var hasClickedVeo = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (currentState == State.IDLE) return
        val root = rootInActiveWindow ?: return

        when (currentState) {
            State.GEMINI_FIND_INPUT -> handleGeminiInput(root)
            State.GEMINI_WAIT_GEN -> handleGeminiWait(root)
            State.INSTA_UPLOAD_UI -> handleInstaUpload(root)
            State.YOUTUBE_UPLOAD_UI -> handleYouTubeUpload(root)
            else -> {}
        }
    }

    private fun handleGeminiInput(root: AccessibilityNodeInfo) {
        // Only try to click Veo once
        if (!hasClickedVeo) {
            val videoOptions = root.findAccessibilityNodeInfosByText("Create video").toMutableList()
            videoOptions.addAll(root.findAccessibilityNodeInfosByText("Veo"))
            val videoButton = videoOptions.firstOrNull { it.isClickable }
            if (videoButton != null) {
                videoButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                hasClickedVeo = true
                return // Wait for next event after clicking
            }
        }

        // Use a robust recursive search to find the very first editable text box on the screen
        val inputNode = findEditableNode(root)
        
        if (inputNode != null) {
            val currentText = inputNode.text?.toString() ?: ""
            // Check if we already pasted our long prompt
            if (!currentText.contains("vertical video", ignoreCase = true)) {
                // Set the text and RETURN. We must wait for the next AccessibilityEvent 
                // so the Gemini UI has time to update the Mic icon into a Send icon.
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, prompt)
                inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                return 
            } else {
                // Text is already pasted and UI has updated. Find the send button!
                val sendNode = findNodeByContentDesc(root, "Send") 
                    ?: findNodeByContentDesc(root, "Submit")
                    ?: findNodeByContentDesc(root, "Message")
                    ?: findNodeByContentDesc(root, "Generate")
                    ?: findNodeByContentDesc(root, "Create")
                    ?: root.findAccessibilityNodeInfosByViewId("com.google.android.apps.bard:id/send_button").firstOrNull()
                    ?: root.findAccessibilityNodeInfosByViewId("com.google.android.apps.bard:id/submit_button").firstOrNull()
                    
                if (sendNode != null) {
                    sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d(TAG, "Prompt sent. Waiting for generation...")
                    currentState = State.GEMINI_WAIT_GEN
                    hasClickedVeo = false // Reset for next run
                }
            }
        }
    }

    // Helper to deeply search the UI tree for any editable text field (ignores guessing placeholder text)
    private fun findEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable || node.className?.toString()?.contains("EditText") == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findEditableNode(child)
            if (result != null) return result
        }
        return null
    }

    // Helper to search by content description (useful for icon buttons without text)
    private fun findNodeByContentDesc(node: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val desc = node.contentDescription?.toString()
        if (desc != null && desc.contains(query, ignoreCase = true)) {
            if (node.isClickable) return node
            if (node.parent?.isClickable == true) return node.parent
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findNodeByContentDesc(child, query)
            if (result != null) return result
        }
        return null
    }

    private fun handleGeminiWait(root: AccessibilityNodeInfo) {
        val downloadNode = root.findAccessibilityNodeInfosByText("Download").firstOrNull { it.isClickable || it.parent?.isClickable == true }
            ?: findNodeByContentDesc(root, "Download")
            ?: findNodeByContentDesc(root, "Save")
            
        if (downloadNode != null) {
            val target = if (downloadNode.isClickable) downloadNode else downloadNode.parent
            target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Download clicked. Polling media directories...")
            
            currentState = State.WAITING_FOR_DOWNLOAD_FILE
            startFilePolling()
        }
    }

    private var lastKnownSize = -1L
    private var sizeStableCount = 0

    private fun startFilePolling() {
        lastKnownSize = -1L
        sizeStableCount = 0
        
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (currentState != State.WAITING_FOR_DOWNLOAD_FILE) return

                val currentLatestFile = getLatestDownloadFile()
                val currentModified = currentLatestFile?.lastModified() ?: -1L
                
                if (currentModified != -1L && currentModified > baselineLastModified) {
                    val currentSize = currentLatestFile!!.length()
                    
                    // Verify the file is actually downloading and has finished
                    if (currentSize > 0 && currentSize == lastKnownSize) {
                        sizeStableCount++
                    } else {
                        lastKnownSize = currentSize
                        sizeStableCount = 0
                    }
                    
                    // If size > 0 and hasn't changed for 2 consecutive checks (4 seconds), it's fully downloaded
                    if (sizeStableCount >= 2) {
                        try {
                            downloadedVideoUri = FileProvider.getUriForFile(
                                applicationContext,
                                "\${applicationContext.packageName}.fileprovider",
                                currentLatestFile
                            )
                            Log.d(TAG, "New video verified! URI: \$downloadedVideoUri")
                            Toast.makeText(applicationContext, "Video Ready! Sharing to Insta...", Toast.LENGTH_SHORT).show()
                            shareToInstagram()
                        } catch (e: Exception) {
                            Log.e(TAG, "FileProvider error: \${e.message}")
                        }
                    } else {
                        Log.d(TAG, "Video still downloading... Size: \$currentSize")
                        handler.postDelayed(this, 2000)
                    }
                } else {
                    // Check again in 2 seconds
                    handler.postDelayed(this, 2000)
                }
            }
        }, 2000)
    }

    private fun getLatestDownloadFile(): File? {
        try {
            val dirsToWatch = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            )
            
            var latestFile: File? = null
            var maxModified = -1L
            
            for (dir in dirsToWatch) {
                if (!dir.exists() || !dir.isDirectory) continue
                
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isDirectory) {
                        val subFiles = file.listFiles() ?: continue
                        for (subFile in subFiles) {
                            if (subFile.extension.equals("mp4", ignoreCase = true) && subFile.lastModified() > maxModified) {
                                maxModified = subFile.lastModified()
                                latestFile = subFile
                            }
                        }
                    } else if (file.extension.equals("mp4", ignoreCase = true) && file.lastModified() > maxModified) {
                        maxModified = file.lastModified()
                        latestFile = file
                    }
                }
            }
            return latestFile
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files: \${e.message}")
        }
        return null
    }
    
    private fun getLatestDownloadTime(): Long {
        return getLatestDownloadFile()?.lastModified() ?: -1L
    }

    private fun shareToInstagram() {
        currentState = State.INSTA_UPLOAD_UI
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, downloadedVideoUri)
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Instagram not installed")
            shareToYouTube() // Fallback
        }
    }

    private fun handleInstaUpload(root: AccessibilityNodeInfo) {
        val captionNodes = root.findAccessibilityNodeInfosByText("Write a caption...")
        if (captionNodes.isNotEmpty()) {
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, caption)
            captionNodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            
            val shareNodes = root.findAccessibilityNodeInfosByText("Share")
            if (shareNodes.isNotEmpty()) {
                shareNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Instagram Shared. Moving to YouTube...")
                
                // Wait for Insta upload to start, then move to YouTube
                handler.postDelayed({ shareToYouTube() }, 3000)
            }
        }
    }

    private fun shareToYouTube() {
        currentState = State.YOUTUBE_UPLOAD_UI
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, downloadedVideoUri)
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "YouTube not installed")
            currentState = State.IDLE
        }
    }

    private fun handleYouTubeUpload(root: AccessibilityNodeInfo) {
        val captionNodes = root.findAccessibilityNodeInfosByText("Caption your Short")
        if (captionNodes.isNotEmpty()) {
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, caption)
            captionNodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            
            val uploadNodes = root.findAccessibilityNodeInfosByText("Upload Short")
            if (uploadNodes.isNotEmpty()) {
                uploadNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                currentState = State.IDLE
                Toast.makeText(applicationContext, "Automation Fully Complete!", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Automation fully complete.")
            }
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Service Interrupted")
        currentState = State.IDLE
    }
}


