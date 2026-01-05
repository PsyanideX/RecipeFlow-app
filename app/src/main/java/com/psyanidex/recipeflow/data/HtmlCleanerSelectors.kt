package com.psyanidex.recipeflow.data

object HtmlCleanerSelectors {
    val aevitar = listOf(
        "script", "img", "video", "audio", "canvas", "style", "header", "footer", 
        "nav", "aside", "iframe", "form", "button", "input", ".ads", ".advertisement", 
        ".ad-container", ".ad", "#comments", "[style*='display: none']", 
        "[style*='visibility: hidden']", ".modal", ".popup", ".share", ".social", 
        ".promo", "[class*='related-posts']", ".newsletter", ".follow", ".cookie",
        "[id*='cookie']", ".footer", ".foot", ".menu", ".search", ".comments", 
        ".deeplink", ".ecommerce", "[class*='related_posts']"
    )

    val especificas = listOf<String>(
        "#menu-menu-definitivo-2"
    )
}
