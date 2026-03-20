package com.peekr.ui.tools

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolWebViewScreen(
    toolId: Long,
    navController: NavController,
    viewModel: ToolsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tool = uiState.tools.find { it.id == toolId }
    var isLoading by remember { mutableStateOf(true) }
    var pageTitle by remember { mutableStateOf(tool?.name ?: "الأداة") }
    var canGoBack by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val htmlPath = tool?.let { viewModel.getPopupPath(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (canGoBack) webViewRef?.goBack()
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (htmlPath == null) {
                // أداة مش موجودة
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text("الأداة مش موجودة أو اتحذف")
                        Button(onClick = { navController.popBackStack() }) {
                            Text("رجوع")
                        }
                    }
                }
            } else {
                // WebView
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewRef = this

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                allowFileAccessFromFileURLs = true
                                allowUniversalAccessFromFileURLs = true
                                cacheMode = WebSettings.LOAD_NO_CACHE
                                setSupportZoom(false)
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrEmpty() && title != "about:blank") {
                                        pageTitle = title
                                    }
                                }

                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    isLoading = newProgress < 100
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    // السماح بالتنقل داخل مجلد الأداة فقط
                                    val url = request?.url?.toString() ?: return false
                                    return !url.startsWith("file://")
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    canGoBack = view?.canGoBack() ?: false
                                }
                            }

                            // تحميل popup.html
                            loadUrl("file://$htmlPath")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // مؤشر التحميل
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}
