package com.example.bmupds

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException

private const val PORTAL_URL = "https://pds.bmu.ac.bd/pds/user_mod/pages/home/index.php"

private val INJECT_JS = """
(function() {

  /* ── 1. VIEWPORT ─────────────────────────────────────────────── */
  var vm = document.querySelector('meta[name="viewport"]');
  if (vm) vm.remove();
  var m = document.createElement('meta');
  m.name = 'viewport';
  m.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
  document.head.appendChild(m);

  /* ── 2. FONTS ─────────────────────────────────────────────────── */
  if (!document.querySelector('#bmu-font')) {
    var lnk = document.createElement('link');
    lnk.id = 'bmu-font'; lnk.rel = 'stylesheet';
    lnk.href = 'https://fonts.googleapis.com/css2?family=Noto+Sans+Bengali:wght@400;500;600;700&family=Inter:wght@400;500;600;700&display=swap';
    document.head.appendChild(lnk);
  }

  /* ── 3. GLOBAL CSS ────────────────────────────────────────────── */
  if (!document.querySelector('#bmu-style')) {
    var st = document.createElement('style');
    st.id = 'bmu-style';
    st.innerHTML = `
      :root {
        --white:  #ffffff;
        --bg:     #f8fafc;
        --border: #e2e8f0;
        --border2:#cbd5e1;
        --text:   #0f172a;
        --text2:  #475569;
        --text3:  #94a3b8;
        --blue:   #2563eb;
        --blue2:  #1d4ed8;
        --fn-en:  'Inter', system-ui, sans-serif;
        --fn-bn:  'Noto Sans Bengali', 'SolaimanLipi', sans-serif;
        --r-sm: 8px; --r-md: 12px; --r-lg: 16px; --r-xl: 20px;
      }

      *, *::before, *::after { box-sizing: border-box !important; }
      html, body {
        width: 100% !important; max-width: 100vw !important;
        margin: 0 !important; padding: 0 !important; overflow-x: hidden !important;
        font-family: var(--fn-bn) !important; background: var(--bg) !important;
      }
      img { max-width: 100% !important; height: auto !important; }
      blink { animation: none !important; }
      [style*="font-size:24px"],[style*="font-size: 24px"],
      [style*="font-size:26px"],[style*="font-size: 26px"],
      [style*="font-size:28px"],[style*="font-size: 28px"] { font-size: inherit !important; }

      table.oe_webclient, .openerp.openerp_webclient_container {
        width: 100% !important; max-width: 100vw !important; min-height: 100vh !important;
      }
      table.oe_webclient > tbody > tr,
      table.oe_webclient > tbody > tr > td { display: block !important; width: 100% !important; }
      table { max-width: none !important; }

      /* TOP BAR */
      .oe_topbar {
        display: flex !important; align-items: center !important;
        justify-content: space-between !important; flex-wrap: nowrap !important;
        width: 100% !important; padding: 0 16px !important; height: 56px !important;
        background: var(--white) !important;
        border-bottom: 1px solid var(--border) !important;
        box-shadow: 0 1px 6px rgba(0,0,0,0.07) !important;
        position: sticky !important; top: 0 !important; z-index: 100 !important;
      }
      .oe_topbar > img { height: 34px !important; width: auto !important; flex-shrink: 0 !important; }
      .oe_systray { display: flex !important; align-items: center !important; gap: 8px !important; flex-shrink: 1 !important; min-width: 0 !important; }
      .oe_topbar_item.oe_topbar_compose_full_email {
        font-size: 12px !important; font-family: var(--fn-bn) !important;
        color: var(--text2) !important; white-space: nowrap !important;
        overflow: hidden !important; text-overflow: ellipsis !important; max-width: 44vw !important;
      }
      .oe_topbar_item img { height: 24px !important; width: auto !important; vertical-align: middle !important; }
      td.oe_topbar[style*="height:5px"], td.oe_topbar[style*="height: 5px"] { display: none !important; }

      /* SIDEBAR — hidden on home page, shown on other pages */
      body.bmu-home td.oe_leftbar { display: none !important; }
      td.oe_leftbar {
        display: block !important; width: 100% !important;
        padding: 12px 12px 14px !important; background: var(--bg) !important;
        border-right: none !important; border-bottom: 1px solid var(--border) !important;
      }
      .oe_secondary_menus_container, .menu_bg, .smartmenu { width: 100% !important; }
      .menu_bg > table, .menu_bg > table > tbody,
      .menu_bg > table > tbody > tr,
      .menu_bg > table > tbody > tr > td { width: 100% !important; display: block !important; }

      /* Flat nav item (হোম পেজ) */
      .flat-menu-item {
        display: flex !important; align-items: center !important; gap: 10px !important;
        width: 100% !important; padding: 13px 14px !important; margin-bottom: 5px !important;
        background: var(--white) !important; color: var(--text) !important;
        font-size: 13.5px !important; font-weight: 500 !important;
        font-family: var(--fn-bn) !important; text-decoration: none !important;
        border-radius: var(--r-md) !important; border: 1px solid var(--border) !important;
        border-left: 3px solid #64748b !important;
        box-shadow: 0 1px 3px rgba(0,0,0,0.04) !important;
      }
      .flat-menu-item:active { background: var(--bg) !important; }

      /* বেতন ও ভাতাদি accordion */
      .bmu-salary-accordion { margin-bottom: 5px !important; }
      .bmu-salary-accordion-header {
        display: flex !important; align-items: center !important;
        justify-content: space-between !important; width: 100% !important;
        padding: 13px 14px !important; background: var(--white) !important;
        color: var(--text) !important; font-size: 13.5px !important;
        font-weight: 600 !important; font-family: var(--fn-bn) !important;
        border-radius: var(--r-md) !important; border: 1px solid var(--border) !important;
        border-left: 3px solid var(--blue) !important;
        box-shadow: 0 1px 3px rgba(0,0,0,0.04) !important;
        cursor: pointer !important;
      }
      .bmu-salary-accordion-header.open {
        border-radius: var(--r-md) var(--r-md) 0 0 !important;
        border-bottom-color: transparent !important;
      }
      .bmu-salary-accordion-header .accordion-arrow { transition: transform 0.2s !important; }
      .bmu-salary-accordion-header.open .accordion-arrow { transform: rotate(180deg) !important; }
      .bmu-salary-accordion-body {
        display: none;
        background: var(--white) !important;
        border: 1px solid var(--border) !important; border-top: none !important;
        border-left: 3px solid var(--blue) !important;
        border-radius: 0 0 var(--r-md) var(--r-md) !important;
        overflow: hidden !important;
        box-shadow: 0 3px 8px rgba(0,0,0,0.05) !important;
      }
      .bmu-salary-sub-item {
        display: flex !important; align-items: center !important; gap: 8px !important;
        width: 100% !important; padding: 12px 14px 12px 20px !important;
        font-size: 13px !important; font-family: var(--fn-bn) !important;
        color: var(--text2) !important; text-decoration: none !important;
        border-bottom: 1px solid var(--border) !important;
      }
      .bmu-salary-sub-item::before {
        content: '›' !important; color: var(--blue) !important;
        font-size: 18px !important; flex-shrink: 0 !important; line-height: 1 !important;
      }
      .bmu-salary-sub-item:last-child { border-bottom: none !important; }
      .bmu-salary-sub-item:active { background: var(--bg) !important; }

      /* আরও অপশন dropdown */
      .custom-bottom-dropdown-container { margin-top: 5px !important; }
      .custom-dropdown-header {
        display: flex !important; align-items: center !important;
        justify-content: space-between !important; width: 100% !important;
        padding: 13px 14px !important; background: var(--white) !important;
        color: var(--text2) !important; font-size: 13.5px !important;
        font-weight: 600 !important; font-family: var(--fn-en) !important;
        border-radius: var(--r-md) !important; border: 1px solid var(--border) !important;
        cursor: pointer !important;
      }
      .custom-dropdown-header.open {
        border-radius: var(--r-md) var(--r-md) 0 0 !important;
        border-color: var(--border2) !important; color: var(--text) !important;
        background: var(--bg) !important;
      }
      .custom-dropdown-header .dropdown-arrow { transition: transform 0.2s !important; }
      .custom-dropdown-header.open .dropdown-arrow { transform: rotate(180deg) !important; }
      .custom-dropdown-content {
        display: none; width: 100% !important; background: var(--white) !important;
        border: 1px solid var(--border) !important; border-top: none !important;
        border-radius: 0 0 var(--r-md) var(--r-md) !important; overflow: hidden !important;
        box-shadow: 0 4px 10px rgba(0,0,0,0.06) !important;
      }
      .custom-dropdown-content a {
        display: flex !important; align-items: center !important; gap: 8px !important;
        width: 100% !important; padding: 12px 14px !important;
        font-size: 13.5px !important; font-family: var(--fn-bn) !important;
        color: var(--text2) !important; text-decoration: none !important;
        border-bottom: 1px solid var(--border) !important;
      }
      .custom-dropdown-content a::before {
        content: '›' !important; color: var(--text3) !important;
        font-size: 18px !important; flex-shrink: 0 !important; line-height: 1 !important;
      }
      .custom-dropdown-content a:last-child { border-bottom: none !important; }
      .custom-dropdown-content a:active { background: var(--bg) !important; }

      /* MAIN CONTENT AREA */
      td.oe_application {
        display: block !important; width: 100% !important; max-width: 100vw !important;
        padding: 0 !important; overflow-x: hidden !important;
        -webkit-overflow-scrolling: touch !important; background: var(--bg) !important;
      }

      /* PAGE HEADER */
      table.oe_view_manager_header {
        width: 100% !important; background: var(--white) !important;
        border-bottom: 1px solid var(--border) !important;
        box-shadow: 0 1px 4px rgba(0,0,0,0.05) !important; table-layout: fixed !important;
      }
      .oe_header_row_top { display: block !important; width: 100% !important; }
      .oe_header_row_top > td {
        display: block !important; width: 100% !important; padding: 12px 14px !important;
      }
      h2.oe_view_title { margin: 0 0 2px !important; }
      .oe_view_title_text a, a.oe_breadcrumb_title {
        font-size: 16px !important; font-family: var(--fn-bn) !important;
        font-weight: 700 !important; color: var(--text) !important; text-decoration: none !important;
      }
      span.oe_breadcrumb_item {
        font-size: 12px !important; font-family: var(--fn-bn) !important; color: var(--text2) !important;
      }
      .oe_header_row_top td[align="right"] { display: block !important; width: 100% !important; padding: 0 14px 14px !important; }
      .oe_header_row_top td[align="right"] > table,
      .oe_header_row_top td > table[width="480"],
      .oe_header_row_top td > table[align="left"] {
        width: 100% !important; float: none !important; table-layout: fixed !important;
        border-radius: var(--r-lg) !important; overflow: hidden !important;
        border: 1px solid var(--border) !important;
      }
      .oe_header_row_top td[width="80"] {
        width: 60px !important; min-width: 60px !important;
        background: #dbeafe !important; text-align: center !important; padding: 10px 6px !important;
        vertical-align: middle !important;
      }
      .oe_header_row_top td[width="80"] img { width: 44px !important; height: auto !important; }
      .oe_header_row_top td[width="400"] {
        width: auto !important; padding: 10px 12px !important;
        background: #eff6ff !important; font-family: var(--fn-en) !important;
        vertical-align: middle !important;
      }
      .oe_header_row_top td[width="400"] span { display: block !important; line-height: 1.5 !important; }
      .oe_header_row_top span[style*="font-size:24px"] {
        font-size: 14px !important; font-weight: 700 !important; color: var(--text) !important;
      }
      .oe_header_row_top span[style*="font-size:18px"],
      .oe_header_row_top span[style*="font-size:16px"] {
        font-size: 12px !important; color: var(--text2) !important;
      }

      /* FORM BUTTONS */
      .oe_form_buttons, .oe_form_buttons_edit {
        display: flex !important; flex-wrap: wrap !important; gap: 8px !important;
        align-items: center !important; padding: 10px 14px !important;
        background: var(--white) !important; border-bottom: 1px solid var(--border) !important;
      }
      .oe_form_buttons > table { display: none !important; }
      .oe_button, .oe_form_button, .oe_form_button_save, .oe_highlight {
        padding: 9px 16px !important; font-size: 13px !important;
        font-family: var(--fn-en) !important; font-weight: 600 !important;
        border-radius: var(--r-sm) !important; white-space: nowrap !important;
        border: 1px solid var(--border2) !important; cursor: pointer !important;
        background: var(--white) !important; color: var(--text) !important;
      }
      .oe_highlight {
        background: var(--blue) !important; color: #fff !important;
        border-color: var(--blue2) !important; box-shadow: 0 2px 8px rgba(37,99,235,0.25) !important;
      }

      /* FORM SHEET */
      .oe_form_sheetbg { display: block !important; width: 100% !important; padding: 10px !important; background: var(--bg) !important; }
      .oe_form_sheet_width, .oe_form_sheet_widthh {
        display: block !important; width: 100% !important;
        overflow-x: hidden !important; -webkit-overflow-scrolling: touch !important;
      }
      table.form_table {
        width: 100% !important; border-collapse: collapse !important;
        background: var(--white) !important;
        border: 1px solid var(--border) !important;
        border-radius: var(--r-lg) !important; overflow: hidden !important;
      }
      table.form_table tr.oe_form_group_row { display: flex !important; flex-wrap: wrap !important; width: 100% !important; border-bottom: 1px solid var(--border) !important; }
      table.form_table tr.oe_form_group_row:last-child { border-bottom: none !important; }
      table.form_table td.oe_form_group_cell_label {
        display: block !important; width: 40% !important;
        padding: 10px 12px !important; font-size: 12px !important;
        font-weight: 600 !important; color: var(--text2) !important;
        font-family: var(--fn-en) !important; background: var(--bg) !important;
        border-right: 1px solid var(--border) !important;
        word-break: break-word !important; vertical-align: middle !important;
      }
      table.form_table td.oe_form_group_cell:not(.oe_form_group_cell_label) {
        display: block !important; width: 60% !important;
        padding: 8px 12px !important; font-size: 13px !important;
        color: var(--text) !important; font-family: var(--fn-bn) !important;
        background: var(--white) !important; vertical-align: middle !important;
        word-break: break-word !important;
      }
      table.form_table td[colspan="5"],
      table.form_table td[colspan="4"] {
        display: block !important; width: 100% !important;
        padding: 10px 14px !important; font-size: 13px !important;
        font-weight: 700 !important; font-family: var(--fn-bn) !important;
        color: var(--text) !important; background: #e8edf5 !important;
        border-bottom: 1px solid var(--border) !important;
      }
      table.form_table input[type="text"],
      table.form_table input[type="date"],
      table.form_table input[type="email"],
      table.form_table input[type="number"],
      table.form_table select,
      table.form_table textarea {
        width: 100% !important; max-width: 100% !important;
        padding: 7px 10px !important; font-size: 13px !important;
        font-family: var(--fn-bn) !important;
        border: 1px solid var(--border2) !important;
        border-radius: var(--r-sm) !important;
        background: var(--white) !important; color: var(--text) !important;
        box-sizing: border-box !important;
      }
      table.oe_form_group:not(.form_table) { width: 100% !important; }
      table.oe_form_group:not(.form_table) > tbody > tr > td.oe_form_group_cell {
        display: block !important; width: 100% !important; padding: 0 !important;
      }

      /* LIST TABLES */
      .oe_list {
        width: 100% !important; display: block !important;
        overflow-x: auto !important; -webkit-overflow-scrolling: touch !important;
      }
      .oe_list_content {
        min-width: max-content !important; border-collapse: collapse !important;
        font-family: var(--fn-bn) !important;
      }
      .oe_list_header_columns th {
        padding: 12px 16px !important; white-space: nowrap !important;
        font-size: 12px !important; font-weight: 700 !important;
        background: var(--bg) !important; color: var(--text2) !important;
        border-bottom: 2px solid var(--border) !important;
        text-transform: uppercase !important; letter-spacing: 0.4px !important;
        font-family: var(--fn-en) !important;
      }
      .oe_list_content tbody tr { border-bottom: 1px solid var(--border) !important; }
      .oe_list_content td {
        padding: 12px 16px !important; font-size: 13px !important;
        vertical-align: middle !important; white-space: nowrap !important;
        color: var(--text) !important;
      }
      .oe_list_content td a {
        font-size: 12px !important; font-weight: 600 !important;
        padding: 4px 12px !important; border-radius: 20px !important;
        display: inline-block !important; white-space: nowrap !important;
      }
      .bmu-list-original-hidden { display: none !important; }

      /* DASHBOARD ICON GRID */
      #bmu-icon-grid {
        display: grid !important; grid-template-columns: 1fr 1fr !important;
        gap: 12px !important; padding: 16px !important;
        width: 100% !important; background: var(--bg) !important;
      }
      .bmu-tile {
        display: flex !important; flex-direction: column !important;
        align-items: center !important; justify-content: center !important;
        padding: 22px 12px 18px !important; text-decoration: none !important;
        background: var(--white) !important; border: 1px solid var(--border) !important;
        border-radius: var(--r-xl) !important; box-shadow: 0 1px 4px rgba(0,0,0,0.06) !important;
        gap: 10px !important; min-height: 110px !important;
        position: relative !important; overflow: hidden !important;
      }
      .bmu-tile::before {
        content: '' !important; position: absolute !important;
        top: 0 !important; left: 0 !important; right: 0 !important; height: 3px !important;
        background: linear-gradient(90deg, var(--blue), #6366f1) !important;
      }
      .bmu-tile:active { background: var(--bg) !important; }
      .bmu-tile-icon { font-size: 30px !important; line-height: 1 !important; }
      .bmu-tile-label {
        font-size: 13px !important; font-weight: 600 !important;
        font-family: var(--fn-bn) !important; color: var(--text) !important;
        text-align: center !important; line-height: 1.4 !important;
      }

      /* FOOTER */
      td[bgcolor="#99CC33"] {
        background: #f1f5f9 !important; color: var(--text3) !important;
        font-size: 11px !important; text-align: center !important; padding: 14px !important;
        border-top: 1px solid var(--border) !important;
      }
      td[bgcolor="#99CC33"] div { color: var(--text3) !important; }

      /* SALARY SLIP CARD */
      #bmu-salary-card { padding: 12px !important; background: var(--bg) !important; font-family: var(--fn-bn) !important; }
      .bmu-slip-meta {
        background: var(--white) !important; border: 1px solid var(--border) !important;
        border-radius: var(--r-xl) !important; overflow: hidden !important;
        margin-bottom: 12px !important; box-shadow: 0 1px 4px rgba(0,0,0,0.05) !important;
      }
      .bmu-slip-meta-title {
        padding: 14px 16px 10px !important; font-size: 13px !important; font-weight: 700 !important;
        font-family: var(--fn-en) !important; color: var(--text) !important;
        border-bottom: 1px solid var(--border) !important; background: var(--bg) !important;
        text-transform: uppercase !important; letter-spacing: 0.5px !important;
      }
      .bmu-slip-meta-grid { display: grid !important; grid-template-columns: 1fr 1fr !important; }
      .bmu-slip-meta-item {
        padding: 10px 14px !important; border-right: 1px solid var(--border) !important;
        border-bottom: 1px solid var(--border) !important;
      }
      .bmu-slip-meta-item:nth-child(even) { border-right: none !important; }
      .bmu-slip-meta-label {
        font-size: 10px !important; font-weight: 700 !important; text-transform: uppercase !important;
        letter-spacing: 0.5px !important; color: var(--text3) !important;
        font-family: var(--fn-en) !important; margin-bottom: 3px !important; display: block !important;
      }
      .bmu-slip-meta-value { font-size: 13px !important; font-weight: 600 !important; color: var(--text) !important; font-family: var(--fn-en) !important; }
      .bmu-slip-section {
        background: var(--white) !important; border: 1px solid var(--border) !important;
        border-radius: var(--r-xl) !important; overflow: hidden !important;
        margin-bottom: 12px !important; box-shadow: 0 1px 4px rgba(0,0,0,0.05) !important;
      }
      .bmu-slip-section-header {
        display: flex !important; align-items: center !important;
        justify-content: space-between !important; padding: 14px 16px !important;
        background: var(--bg) !important; border-bottom: 1px solid var(--border) !important;
      }
      .bmu-slip-section-title {
        font-size: 12px !important; font-weight: 700 !important;
        font-family: var(--fn-en) !important; color: var(--text) !important;
        text-transform: uppercase !important; letter-spacing: 0.5px !important;
      }
      .bmu-slip-section-total { font-size: 16px !important; font-weight: 700 !important; font-family: var(--fn-en) !important; }
      .bmu-slip-section-total.earn  { color: #059669 !important; }
      .bmu-slip-section-total.deduct{ color: #dc2626 !important; }
      .bmu-slip-row {
        display: flex !important; align-items: center !important;
        justify-content: space-between !important; padding: 11px 16px !important;
        border-bottom: 1px solid #f1f5f9 !important;
      }
      .bmu-slip-row:last-child { border-bottom: none !important; }
      .bmu-slip-row.zero { opacity: 0.38 !important; }
      .bmu-slip-row-label { font-size: 13.5px !important; color: var(--text) !important; font-family: var(--fn-bn) !important; flex: 1 !important; padding-right: 12px !important; }
      .bmu-slip-row-amount { font-size: 14px !important; font-weight: 600 !important; font-family: var(--fn-en) !important; color: var(--text) !important; white-space: nowrap !important; }
      .bmu-slip-row-amount.zero-val { color: var(--text3) !important; font-weight: 400 !important; }
      .bmu-slip-net {
        background: var(--text) !important; border-radius: var(--r-xl) !important;
        padding: 20px !important; margin-bottom: 12px !important;
        display: flex !important; align-items: center !important; justify-content: space-between !important;
        box-shadow: 0 4px 16px rgba(15,23,42,0.2) !important;
      }
      .bmu-slip-net-label { font-size: 12px !important; font-weight: 600 !important; color: rgba(255,255,255,0.6) !important; font-family: var(--fn-en) !important; margin-bottom: 4px !important; display: block !important; }
      .bmu-slip-net-amount { font-size: 26px !important; font-weight: 700 !important; color: #fff !important; font-family: var(--fn-en) !important; letter-spacing: -0.5px !important; }
      .bmu-slip-net-badge { background: rgba(255,255,255,0.12) !important; border-radius: 12px !important; padding: 8px 14px !important; text-align: center !important; }
      .bmu-slip-net-badge-label { font-size: 10px !important; color: rgba(255,255,255,0.5) !important; font-family: var(--fn-en) !important; display: block !important; text-transform: uppercase !important; letter-spacing: 0.5px !important; margin-bottom: 3px !important; }
      .bmu-slip-net-badge-val { font-size: 13px !important; font-weight: 700 !important; color: #fff !important; font-family: var(--fn-en) !important; }
      .bmu-slip-inwords { background: var(--white) !important; border: 1px solid var(--border) !important; border-radius: var(--r-lg) !important; padding: 14px 16px !important; margin-bottom: 12px !important; }
      .bmu-slip-inwords-label { font-size: 10px !important; font-weight: 700 !important; text-transform: uppercase !important; letter-spacing: 0.5px !important; color: var(--text3) !important; font-family: var(--fn-en) !important; margin-bottom: 6px !important; display: block !important; }
      .bmu-slip-inwords-text { font-size: 14px !important; color: var(--text) !important; font-family: var(--fn-bn) !important; line-height: 1.5 !important; }

      /* SALARY LIST CARDS */
      #bmu-list-card {
        padding: 12px !important; background: var(--bg) !important;
        font-family: var(--fn-bn) !important;
        width: 100% !important; max-width: 100vw !important;
        overflow-x: hidden !important; box-sizing: border-box !important;
      }
      .bmu-list-title-bar {
        background: var(--white) !important; border: 1px solid var(--border) !important;
        border-radius: var(--r-xl) !important; padding: 14px 16px !important;
        margin-bottom: 12px !important; box-shadow: 0 1px 4px rgba(0,0,0,0.05) !important;
        display: flex !important; align-items: center !important; gap: 12px !important;
        width: 100% !important; box-sizing: border-box !important;
      }
      .bmu-list-title-icon { font-size: 24px !important; flex-shrink: 0 !important; }
      .bmu-list-title-text { font-size: 15px !important; font-weight: 700 !important; color: var(--text) !important; font-family: var(--fn-bn) !important; line-height: 1.4 !important; }
      .bmu-list-title-sub { font-size: 11px !important; color: var(--text3) !important; font-family: var(--fn-en) !important; margin-top: 2px !important; display: block !important; }
      .bmu-list-empty {
        background: var(--white) !important; border: 1px solid var(--border) !important;
        border-radius: var(--r-xl) !important; padding: 40px 20px !important;
        text-align: center !important; box-shadow: 0 1px 4px rgba(0,0,0,0.05) !important;
        width: 100% !important; box-sizing: border-box !important;
      }
      .bmu-list-empty-icon { font-size: 36px !important; margin-bottom: 12px !important; }
      .bmu-list-empty-text { font-size: 14px !important; color: var(--text3) !important; font-family: var(--fn-bn) !important; }
      .bmu-list-row-card {
        background: var(--white) !important; border: 1px solid var(--border) !important;
        border-radius: var(--r-xl) !important; overflow: hidden !important;
        margin-bottom: 10px !important; box-shadow: 0 1px 4px rgba(0,0,0,0.05) !important;
        position: relative !important;
        width: 100% !important; box-sizing: border-box !important;
        contain: layout !important;
      }
      .bmu-list-row-card::before {
        content: '' !important; position: absolute !important;
        top: 0 !important; left: 0 !important; bottom: 0 !important; width: 3px !important;
        background: var(--blue) !important; border-radius: 3px 0 0 3px !important;
      }
      .bmu-list-row-primary {
        display: flex !important; align-items: center !important;
        justify-content: space-between !important;
        padding: 14px 16px 10px 20px !important;
        border-bottom: 1px solid #f1f5f9 !important;
        width: 100% !important; box-sizing: border-box !important; min-width: 0 !important;
      }
      .bmu-list-row-month {
        font-size: 15px !important; font-weight: 700 !important;
        color: var(--text) !important; font-family: var(--fn-bn) !important;
        flex: 1 !important; min-width: 0 !important;
        overflow: hidden !important; text-overflow: ellipsis !important;
        white-space: nowrap !important; padding-right: 8px !important;
      }
      .bmu-list-row-net {
        font-size: 15px !important; font-weight: 700 !important;
        color: #059669 !important; font-family: var(--fn-en) !important;
        flex-shrink: 0 !important; white-space: nowrap !important;
      }
      .bmu-list-row-grid {
        display: grid !important; grid-template-columns: 1fr 1fr !important;
        width: 100% !important; box-sizing: border-box !important;
      }
      .bmu-list-row-field {
        padding: 9px 12px 9px 20px !important;
        border-right: 1px solid #f1f5f9 !important;
        border-bottom: 1px solid #f1f5f9 !important;
        min-width: 0 !important; overflow: hidden !important; box-sizing: border-box !important;
      }
      .bmu-list-row-field:nth-child(even) { border-right: none !important; padding-left: 12px !important; }
      .bmu-list-row-field:nth-last-child(-n+2) { border-bottom: none !important; }
      .bmu-list-row-field-label {
        font-size: 10px !important; font-weight: 700 !important;
        text-transform: uppercase !important; letter-spacing: 0.5px !important;
        color: var(--text3) !important; font-family: var(--fn-en) !important;
        margin-bottom: 3px !important; display: block !important; white-space: nowrap !important;
      }
      .bmu-list-row-field-value {
        font-size: 13px !important; font-weight: 600 !important;
        color: var(--text) !important; font-family: var(--fn-en) !important;
        white-space: nowrap !important; overflow: hidden !important;
        text-overflow: ellipsis !important; display: block !important;
      }
      .bmu-list-row-field-value.deduct { color: #dc2626 !important; }
      .bmu-status-badge {
        display: inline-block !important; font-size: 11px !important;
        font-weight: 600 !important; font-family: var(--fn-en) !important;
        padding: 3px 10px !important; border-radius: 20px !important;
        background: #f1f5f9 !important; color: var(--text2) !important; white-space: nowrap !important;
      }
      .bmu-status-badge.pending  { background: #fef3c7 !important; color: #92400e !important; }
      .bmu-status-badge.approved { background: #d1fae5 !important; color: #065f46 !important; }
      .bmu-status-badge.rejected { background: #fee2e2 !important; color: #991b1b !important; }
      .bmu-status-badge.paid     { background: #dbeafe !important; color: #1e40af !important; }
      .bmu-list-row-action {
        display: block !important; margin: 10px 16px 12px 20px !important;
        padding: 10px 16px !important; text-align: center !important;
        background: var(--blue) !important; color: #fff !important;
        font-size: 13px !important; font-weight: 600 !important;
        font-family: var(--fn-en) !important; text-decoration: none !important;
        border-radius: var(--r-md) !important;
        box-shadow: 0 2px 8px rgba(37,99,235,0.25) !important;
        box-sizing: border-box !important; width: calc(100% - 36px) !important;
      }
      .bmu-list-row-action:active { opacity: 0.85 !important; }

      /* REPORT FORM */
      #bmu-report-form-wrap {
        background: var(--white) !important; border: 1px solid var(--border) !important;
        border-radius: var(--r-xl) !important; padding: 16px !important;
        margin-bottom: 12px !important; box-shadow: 0 1px 4px rgba(0,0,0,0.05) !important;
        width: 100% !important; box-sizing: border-box !important;
      }
      #bmu-report-form-wrap select,
      #bmu-report-form-wrap input[type="text"] {
        width: 100% !important; padding: 10px 12px !important;
        border: 1px solid var(--border2) !important; border-radius: var(--r-sm) !important;
        font-size: 14px !important; font-family: var(--fn-en) !important;
        background: var(--white) !important; color: var(--text) !important;
        margin-top: 6px !important; box-sizing: border-box !important;
      }
      #bmu-report-form-wrap input[type="submit"],
      #bmu-report-form-wrap button[type="submit"],
      #bmu-report-form-wrap input[name="submit"] {
        width: 100% !important; padding: 13px !important; margin-top: 14px !important;
        background: var(--blue) !important; color: #fff !important;
        border: none !important; border-radius: var(--r-md) !important;
        font-size: 15px !important; font-weight: 700 !important;
        font-family: var(--fn-en) !important; cursor: pointer !important;
        box-shadow: 0 2px 8px rgba(37,99,235,0.3) !important; box-sizing: border-box !important;
      }
      #bmu-report-form-wrap label {
        display: block !important; font-size: 12px !important; font-weight: 600 !important;
        color: var(--text2) !important; font-family: var(--fn-en) !important; margin-top: 12px !important;
      }
      #bmu-report-form-wrap .report-option {
        display: flex !important; align-items: center !important; gap: 10px !important;
        padding: 11px 14px !important; margin-top: 8px !important;
        border: 1px solid var(--border) !important; border-radius: var(--r-md) !important;
        background: var(--bg) !important; cursor: pointer !important; box-sizing: border-box !important;
      }
      #bmu-report-form-wrap .report-option input[type="radio"] { width: 18px !important; height: 18px !important; flex-shrink: 0 !important; }
      #bmu-report-form-wrap .report-option strong { font-size: 13.5px !important; font-family: var(--fn-en) !important; color: var(--text) !important; }

      /* LOGIN PAGE */
      html:has(.oe_login_signup), body:has(.oe_login_signup) { height: 100% !important; }
      .openerp.openerp_webclient_container:has(.oe_login_signup),
      table.oe_webclient:has(.oe_login_signup),
      table.oe_webclient:has(.oe_login_signup) > tbody,
      table.oe_webclient:has(.oe_login_signup) > tbody > tr,
      table.oe_webclient:has(.oe_login_signup) > tbody > tr > td {
        display: block !important; width: 100% !important;
        background: transparent !important; border: none !important;
        padding: 0 !important; margin: 0 !important;
      }
      .oe_webclient:has(.oe_login_signup) .oe_topbar,
      .oe_webclient:has(.oe_login_signup) td.oe_topbar,
      .oe_webclient:has(.oe_login_signup) td.oe_leftbar { display: none !important; }
      .oe_enterprise.oe_login_signup {
        display: flex !important; align-items: center !important; justify-content: center !important;
        min-height: 100vh !important; width: 100% !important; padding: 32px 20px !important;
        background:
          radial-gradient(ellipse at 20% 20%, rgba(37,99,235,0.18) 0%, transparent 60%),
          radial-gradient(ellipse at 80% 80%, rgba(99,102,241,0.14) 0%, transparent 55%),
          linear-gradient(145deg, #0a0f1e 0%, #0f172a 40%, #1a1f35 100%) !important;
        overflow: hidden !important; position: relative !important;
      }
      .oe_enterprise.oe_login_signup::before {
        content: '' !important; position: fixed !important;
        top: -80px !important; right: -80px !important; width: 280px !important; height: 280px !important;
        background: radial-gradient(circle, rgba(37,99,235,0.22) 0%, transparent 70%) !important;
        border-radius: 50% !important; pointer-events: none !important; z-index: 0 !important;
      }
      .oe_enterprise.oe_login_signup::after {
        content: '' !important; position: fixed !important;
        bottom: -60px !important; left: -60px !important; width: 220px !important; height: 220px !important;
        background: radial-gradient(circle, rgba(99,102,241,0.18) 0%, transparent 70%) !important;
        border-radius: 50% !important; pointer-events: none !important; z-index: 0 !important;
      }
      .oe_enterprise_content {
        height: auto !important; width: 100% !important; max-width: 420px !important;
        background: transparent !important; background-image: none !important;
        display: flex !important; align-items: center !important; justify-content: center !important;
        position: relative !important; z-index: 1 !important;
      }
      .oe_enterprise_background_header { display: none !important; }
      .oe_login_pane.oe_enterprise_pane {
        position: relative !important; top: 0 !important;
        width: 100% !important; max-width: 400px !important; margin: 0 auto !important; padding: 0 !important;
        background: rgba(255,255,255,0.06) !important; background-image: none !important;
        border: 1px solid rgba(255,255,255,0.10) !important; border-radius: 24px !important;
        box-shadow: 0 0 0 1px rgba(255,255,255,0.04), 0 20px 60px rgba(0,0,0,0.55),
                    0 8px 24px rgba(0,0,0,0.30), inset 0 1px 0 rgba(255,255,255,0.08) !important;
        overflow: hidden !important;
        backdrop-filter: blur(20px) !important; -webkit-backdrop-filter: blur(20px) !important;
      }
      .oe_enterprise_pane h2 { font-size: 0 !important; margin: 0 !important; padding: 0 !important; line-height: 0 !important; height: 0 !important; overflow: hidden !important; }
      .oe_enterprise_pane > form::before {
        content: '' !important; display: block !important; width: 100% !important; height: 110px !important;
        background: linear-gradient(135deg, #1e3a5f 0%, #1a237e 50%, #0d47a1 100%) !important;
      }
      .oe_enterprise_pane > form::after {
        content: '🏛' !important; position: absolute !important;
        top: 62px !important; left: 50% !important; transform: translateX(-50%) !important;
        width: 64px !important; height: 64px !important;
        background: linear-gradient(135deg, #2563eb, #1e40af) !important;
        border-radius: 50% !important; border: 3px solid rgba(255,255,255,0.15) !important;
        box-shadow: 0 4px 20px rgba(37,99,235,0.5) !important;
        font-size: 28px !important; line-height: 64px !important; text-align: center !important; z-index: 2 !important;
      }
      .oe_enterprise_pane > form { position: relative !important; margin: 0 !important; padding: 0 !important; display: block !important; }
      #bmu-login-title { display: block !important; text-align: center !important; padding: 44px 28px 4px !important; font-size: 20px !important; font-weight: 700 !important; font-family: var(--fn-en) !important; color: #fff !important; letter-spacing: 0.3px !important; }
      #bmu-login-subtitle { display: block !important; text-align: center !important; padding: 0 28px 20px !important; font-size: 13px !important; color: rgba(255,255,255,0.5) !important; font-family: var(--fn-en) !important; }
      .oe_enterprise_pane > form > p { display: none !important; }
      .oe_enterprise_bottom, .oe_login_dbpane { display: none !important; }
      .oe_enterprise_pane fieldset { border: none !important; padding: 0 20px !important; margin: 0 !important; }
      .oe_enterprise_pane fieldset label { display: block !important; font-size: 11px !important; font-weight: 600 !important; text-transform: uppercase !important; letter-spacing: 0.8px !important; color: rgba(255,255,255,0.45) !important; font-family: var(--fn-en) !important; margin: 18px 0 6px !important; padding: 0 !important; }
      .oe_enterprise_login_input,
      .oe_enterprise_pane fieldset input[type="text"],
      .oe_enterprise_pane fieldset input[type="password"] {
        display: block !important; width: 100% !important; height: 52px !important;
        padding: 0 16px !important; margin: 0 0 12px 0 !important; font-size: 16px !important;
        font-family: var(--fn-en) !important; color: #fff !important;
        background: rgba(255,255,255,0.07) !important; background-image: none !important;
        border: 1.5px solid rgba(255,255,255,0.12) !important; border-radius: 12px !important;
        outline: none !important; text-shadow: none !important; caret-color: #60a5fa !important;
      }
      .oe_enterprise_login_input:focus,
      .oe_enterprise_pane fieldset input:focus {
        border-color: rgba(96,165,250,0.7) !important; background: rgba(255,255,255,0.11) !important;
        box-shadow: 0 0 0 3px rgba(37,99,235,0.25) !important;
      }
      .oe_login_error_message.oe_enterprise_error_message {
        margin: 0 20px 12px !important; padding: 10px 14px !important;
        background: rgba(239,68,68,0.12) !important; border: 1px solid rgba(239,68,68,0.3) !important;
        border-radius: 10px !important; font-size: 13px !important; color: #fca5a5 !important; font-family: var(--fn-en) !important;
      }
      .oe_login_error_message.oe_enterprise_error_message:empty { display: none !important; }
      .oe_enterprise_submit { padding: 64px 20px 48px !important; margin: 0 !important; }
      .oe_enterprise_submit button[name="submit"] {
        display: block !important; width: 100% !important; height: 54px !important;
        padding: 0 !important; margin: 0 !important; float: none !important;
        font-size: 16px !important; font-weight: 700 !important; font-family: var(--fn-en) !important;
        letter-spacing: 0.5px !important; color: #fff !important;
        background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 50%, #1e40af 100%) !important;
        border: none !important; border-radius: 14px !important; cursor: pointer !important; text-shadow: none !important;
        box-shadow: 0 1px 0 rgba(255,255,255,0.15) inset, 0 6px 24px rgba(37,99,235,0.50), 0 2px 8px rgba(0,0,0,0.30) !important;
      }
      .oe_enterprise_submit button[name="submit"]:active { opacity: 0.88 !important; }
    `;
    document.head.appendChild(st);
  }

  /* ════════════════════════════════════════════════════════════════
     4. SALARY SLIP REBUILDER
  ════════════════════════════════════════════════════════════════ */
  function rebuildSalaryPage() {
    if (document.getElementById('bmu-salary-card')) return;
    if (!window.location.href.includes('salary_ration_money')) return;
    if (!window.location.href.includes('employee_salary_form_view') &&
        !window.location.href.includes('employee_salary_form-3') &&
        !window.location.href.includes('employee_salary_form_festival')) return;

    var formBody = document.querySelector('.oe_view_manager_body');
    if (!formBody) return;
    var tables = Array.prototype.slice.call(formBody.querySelectorAll('table.oe_form_group'));
    if (!tables.length) return;

    function cellText(td) { if (!td) return ''; return td.textContent.replace(/\s+/g, ' ').trim(); }
    function hasValue(str) {
      var clean = str.replace(/[০-৯]/g, function(c) { return String.fromCharCode(c.charCodeAt(0) - 0x09E6 + 48); }).trim();
      return !isNaN(parseFloat(clean)) && parseFloat(clean) !== 0;
    }
    function fmtAmount(str) { if (!str || str.trim() === '' || str.trim() === '\u00a0') return ''; return str.trim(); }
    function isGrayCell(c) { var bg = (c.getAttribute('bgcolor') || '').toUpperCase(); return bg === '#E8E8E8' || bg === 'E8E8E8'; }
    function isSalaryTable(tbl) {
      var text = tbl.textContent || '';
      return text.includes('সর্বমোট') || text.includes('মুল বেতন') || text.includes('কর্তন সমূহ') || text.includes('নীট প্রদেয়');
    }
    function isMetaTable(tbl) {
      if (isSalaryTable(tbl)) return false;
      var rows = Array.prototype.slice.call(tbl.querySelectorAll('tr'));
      var dataRows = 0, grayDataRows = 0;
      rows.forEach(function(r) {
        var cells = Array.prototype.slice.call(r.cells);
        if (cells.length < 2) return;
        var label = cellText(cells[0]);
        if (!label || label.includes('PRINT')) return;
        dataRows++;
        var grayCount = 0;
        cells.forEach(function(c) { if (isGrayCell(c)) grayCount++; });
        if (grayCount >= Math.min(3, cells.length)) grayDataRows++;
      });
      return dataRows >= 3 && grayDataRows > dataRows * 0.6;
    }
    function addMetaPair(labelCell, valueCell) {
      var lbl = cellText(labelCell).replace(/[:：]/g, '').replace(/&nbsp;/g, '').trim();
      var val = cellText(valueCell).replace(/&nbsp;/g, '').trim();
      if (lbl && val && val !== '\u00a0' && lbl.length > 1 && !lbl.includes('PRINT'))
        metaItems.push({ label: lbl, value: val });
    }
    function parseMetaTable(tbl) {
      Array.prototype.slice.call(tbl.querySelectorAll('tr')).forEach(function(row) {
        var cells = Array.prototype.slice.call(row.cells);
        if (cells.length >= 4) {
          addMetaPair(cells[0], cells[1]);
          addMetaPair(cells[cells.length - 2], cells[cells.length - 1]);
        } else if (cells.length >= 2) {
          for (var i = 0; i < cells.length - 1; i += 2) addMetaPair(cells[i], cells[i + 1]);
        }
      });
    }
    function isAmount(str) {
      if (!str || str === '\u00a0') return false;
      var clean = str.replace(/[০-৯]/g, function(c) { return String.fromCharCode(c.charCodeAt(0) - 0x09E6 + 48); }).replace(/,/g, '').trim();
      return /^-?\d+(\.\d+)?$/.test(clean);
    }

    var metaItems = [];
    var salaryTables = [];
    tables.forEach(function(tbl) {
      if (isMetaTable(tbl)) parseMetaTable(tbl);
      else salaryTables.push(tbl);
    });

    var earnings = [], deductions = [];
    var totalEarn = '', totalDeduct = '', netPay = '', inWords = '';

    salaryTables.forEach(function(tbl) {
      var section = 'earnings';
      Array.prototype.slice.call(tbl.querySelectorAll('tr')).forEach(function(row) {
        var cells = Array.prototype.slice.call(row.cells);
        if (!cells.length) return;
        var label = cellText(cells[0]).replace(/&nbsp;/g, '').trim();
        if (cells.length === 1) {
          if (label.includes('কর্তন সমূহ')) section = 'deductions';
          if (label.toLowerCase().includes('inwords') || label.includes('কথায়'))
            inWords = label.replace(/Inwords\s*[:：]?/i, '').trim();
          return;
        }
        if (!label || label === '\u00a0') return;
        if (label.includes('বিবরণ') && /টাকা/.test(cellText(row))) return;
        if (label.includes('কর্তন সমূহ')) { section = 'deductions'; return; }
        if (cells.length >= 5) {
          var col1 = fmtAmount(cellText(cells[1]));
          var col3 = fmtAmount(cellText(cells[3]));
          var col4 = fmtAmount(cellText(cells[4]));
          if (label.includes('সর্বমোট')) { totalEarn = col3 || col1; section = 'deductions'; return; }
          if (label.includes('মোট কর্তন')) { totalDeduct = col1; section = 'done'; return; }
          if (label.includes('নীট প্রদেয়')) {
            netPay = (isAmount(col4) ? col4 : '') || (isAmount(col1) ? col1 : '') || (isAmount(col3) ? col3 : ''); return;
          }
          if (label.toLowerCase().includes('inwords') || label.includes('কথায়')) {
            inWords = label.replace(/Inwords\s*[:：]?/i, '').trim(); return;
          }
          if (section === 'earnings') earnings.push({ label: label, amount: (isAmount(col3) ? col3 : '0.00') });
          else if (section === 'deductions') deductions.push({ label: label, amount: (isAmount(col1) ? col1 : '0.00') });
        }
      });
    });

    var card = document.createElement('div');
    card.id = 'bmu-salary-card';

    if (metaItems.length) {
      var metaCard = document.createElement('div');
      metaCard.className = 'bmu-slip-meta';
      var metaTitle = document.createElement('div');
      metaTitle.className = 'bmu-slip-meta-title';
      metaTitle.textContent = 'বেতন বিল তথ্য';
      metaCard.appendChild(metaTitle);
      var metaGrid = document.createElement('div');
      metaGrid.className = 'bmu-slip-meta-grid';
      var priority = ['নাম','পিডিএস নং','পদবী','বিভাগ','মাস','বছর','ব্যাংক হিসাব'];
      var shown = {}, ordered = [];
      priority.forEach(function(p) {
        metaItems.forEach(function(item) { if (!shown[item.label] && item.label.includes(p)) { ordered.push(item); shown[item.label] = true; } });
      });
      metaItems.forEach(function(item) { if (!shown[item.label]) { ordered.push(item); shown[item.label] = true; } });
      ordered.forEach(function(item) {
        var cell = document.createElement('div');
        cell.className = 'bmu-slip-meta-item';
        cell.innerHTML = '<span class="bmu-slip-meta-label">' + item.label + '</span><span class="bmu-slip-meta-value">' + item.value + '</span>';
        metaGrid.appendChild(cell);
      });
      metaCard.appendChild(metaGrid);
      card.appendChild(metaCard);
    }
    if (earnings.length || totalEarn) {
      var eSection = document.createElement('div');
      eSection.className = 'bmu-slip-section';
      eSection.innerHTML = '<div class="bmu-slip-section-header"><span class="bmu-slip-section-title">আয় (Earnings)</span><span class="bmu-slip-section-total earn">৳ ' + (totalEarn || '—') + '</span></div>';
      earnings.forEach(function(item) {
        var isZero = !hasValue(item.amount);
        var row = document.createElement('div');
        row.className = 'bmu-slip-row' + (isZero ? ' zero' : '');
        row.innerHTML = '<span class="bmu-slip-row-label">' + item.label + '</span><span class="bmu-slip-row-amount' + (isZero ? ' zero-val' : '') + '">৳ ' + item.amount + '</span>';
        eSection.appendChild(row);
      });
      card.appendChild(eSection);
    }
    if (deductions.length || totalDeduct) {
      var dSection = document.createElement('div');
      dSection.className = 'bmu-slip-section';
      dSection.innerHTML = '<div class="bmu-slip-section-header"><span class="bmu-slip-section-title">কর্তন (Deductions)</span><span class="bmu-slip-section-total deduct">৳ ' + (totalDeduct || '—') + '</span></div>';
      deductions.forEach(function(item) {
        var isZero = !hasValue(item.amount);
        var row = document.createElement('div');
        row.className = 'bmu-slip-row' + (isZero ? ' zero' : '');
        row.innerHTML = '<span class="bmu-slip-row-label">' + item.label + '</span><span class="bmu-slip-row-amount' + (isZero ? ' zero-val' : '') + '">৳ ' + item.amount + '</span>';
        dSection.appendChild(row);
      });
      card.appendChild(dSection);
    }
    if (netPay) {
      var netCard = document.createElement('div');
      netCard.className = 'bmu-slip-net';
      netCard.innerHTML = '<div><span class="bmu-slip-net-label">নীট প্রদেয় বেতন</span><span class="bmu-slip-net-amount">৳ ' + netPay + '</span></div><div class="bmu-slip-net-badge"><span class="bmu-slip-net-badge-label">Gross</span><span class="bmu-slip-net-badge-val">৳ ' + (totalEarn || '—') + '</span></div>';
      card.appendChild(netCard);
    }
    if (inWords) {
      var wordsCard = document.createElement('div');
      wordsCard.className = 'bmu-slip-inwords';
      wordsCard.innerHTML = '<span class="bmu-slip-inwords-label">In Words</span><span class="bmu-slip-inwords-text">' + inWords + '</span>';
      card.appendChild(wordsCard);
    }
    formBody.innerHTML = '';
    formBody.appendChild(card);
  }

  /* ════════════════════════════════════════════════════════════════
     5. SALARY LIST PAGE REBUILDER
  ════════════════════════════════════════════════════════════════ */
  function rebuildSalaryListPage() {
    if (document.getElementById('bmu-list-card')) return;
    var url = window.location.href;
    var isSalaryList =
      url.includes('salary_money_monthly_list') ||
      url.includes('salary_money_festival_list') ||
      url.includes('salary_money_monthly_saved_list') ||
      url.includes('salary_money_future_fund') ||
      url.includes('salary_money_monthly_list_by_status') ||
      url.includes('salary_money_monthly_returned_list') ||
      url.includes('advance_report_user_salary');
    if (!isSalaryList) return;

    var pageTitle = 'বেতন ও ভাতাদি', pageIcon = '💰', pageSub = 'salary_ration_money';
    if (url.includes('salary_money_monthly_list_by_status'))  { pageTitle = 'বেতন ও ভাতার অবস্থা'; pageIcon = '📊'; }
    else if (url.includes('salary_money_monthly_saved_list')) { pageTitle = 'সংরক্ষিত বিলের তালিকা'; pageIcon = '🗂️'; }
    else if (url.includes('salary_money_monthly_returned_list')) { pageTitle = 'বেতন ও ভাতাদি পুনরায় প্রেরণ'; pageIcon = '↩️'; }
    else if (url.includes('salary_money_monthly_list'))       { pageTitle = 'প্রক্রিয়াধীন বেতন ও ভাতা'; pageIcon = '📋'; }
    else if (url.includes('salary_money_festival_list'))      { pageTitle = 'প্রক্রিয়াধীন উৎসব ভাতা'; pageIcon = '🎉'; }
    else if (url.includes('salary_money_future_fund'))        { pageTitle = 'ভবিষ্যৎ তহবিল'; pageIcon = '🏦'; }
    else if (url.includes('advance_report_user_salary'))      { pageTitle = 'রিপোর্ট'; pageIcon = '📑'; pageSub = 'Advance Reporting'; }

    function ct(td) { if (!td) return ''; return td.textContent.replace(/\s+/g, ' ').trim(); }
    function statusClass(text) {
      var t = text.toLowerCase();
      if (t.includes('approved') || t.includes('অনুমোদিত') || t.includes('paid') || t.includes('প্রদত্ত')) return 'approved';
      if (t.includes('pending')  || t.includes('প্রক্রিয়া') || t.includes('অপেক্ষ')) return 'pending';
      if (t.includes('rejected') || t.includes('প্রত্যাখ্যান')) return 'rejected';
      if (t.includes('paid')     || t.includes('পরিশোধ')) return 'paid';
      return '';
    }

    var appArea = document.querySelector('td.oe_application');
    if (!appArea) return;
    var listWrappers = Array.prototype.slice.call(appArea.querySelectorAll('.oe_list'));
    if (!listWrappers.length) return;

    var outerCard = document.createElement('div');
    outerCard.id = 'bmu-list-card';

    var titleBar = document.createElement('div');
    titleBar.className = 'bmu-list-title-bar';
    titleBar.innerHTML = '<span class="bmu-list-title-icon">' + pageIcon + '</span><div><div class="bmu-list-title-text">' + pageTitle + '</div><span class="bmu-list-title-sub">' + pageSub + '</span></div>';
    outerCard.appendChild(titleBar);

    if (url.includes('advance_report_user_salary')) {
      var originalForm = appArea.querySelector('form');
      if (originalForm) {
        var formWrap = document.createElement('div');
        formWrap.id = 'bmu-report-form-wrap';
        var select    = originalForm.querySelector('select[name="fiscal_year"]');
        var checkbox  = originalForm.querySelector('input[name="allmonths"]');
        var radios    = Array.prototype.slice.call(originalForm.querySelectorAll('input[type="radio"]'));
        var submitBtn = originalForm.querySelector('input[name="submit"]');
        if (select) {
          var selLbl = document.createElement('label');
          selLbl.textContent = 'অর্থবছর (Fiscal Year)';
          formWrap.appendChild(selLbl);
          formWrap.appendChild(select);
        }
        if (checkbox) {
          var cbWrap = document.createElement('div');
          cbWrap.className = 'report-option';
          var cbStrong = document.createElement('strong');
          cbStrong.textContent = 'Show All Months';
          cbWrap.appendChild(checkbox);
          cbWrap.appendChild(cbStrong);
          formWrap.appendChild(cbWrap);
        }
        if (radios.length) {
          var rLbl = document.createElement('label');
          rLbl.textContent = 'Report Type';
          formWrap.appendChild(rLbl);
          radios.forEach(function(r) {
            var rWrap = document.createElement('div');
            rWrap.className = 'report-option';
            var labelText = '', next = r.nextSibling;
            while (next) {
              if (next.nodeType === 1 && next.tagName === 'STRONG') { labelText = next.textContent.trim(); break; }
              if (next.nodeType === 3 && next.textContent.trim())   { labelText = next.textContent.trim(); break; }
              next = next.nextSibling;
            }
            var rStrong = document.createElement('strong');
            rStrong.textContent = labelText || ('Option ' + r.value);
            rWrap.appendChild(r);
            rWrap.appendChild(rStrong);
            formWrap.appendChild(rWrap);
          });
        }
        if (submitBtn) formWrap.appendChild(submitBtn);
        outerCard.appendChild(formWrap);
      }
    }

    listWrappers.forEach(function(wrapper) {
      var tbl = wrapper.querySelector('table.oe_list_content');
      if (!tbl) return;
      wrapper.classList.add('bmu-list-original-hidden');
      var headerRow = tbl.querySelector('tr.oe_list_header_columns');
      if (!headerRow) return;
      if (tbl.querySelector('input[type="radio"], select[name="fiscal_year"]')) return;
      var headers = Array.prototype.slice.call(headerRow.querySelectorAll('th')).map(function(th) { return th.textContent.replace(/\s+/g, ' ').trim(); });
      var bodyRows = Array.prototype.slice.call(tbl.querySelectorAll('tbody tr')).filter(function(r) { return r.cells && r.cells.length > 0; });
      if (!bodyRows.length) {
        var emptyDiv = document.createElement('div');
        emptyDiv.className = 'bmu-list-empty';
        emptyDiv.innerHTML = '<div class="bmu-list-empty-icon">📭</div><div class="bmu-list-empty-text">কোনো তথ্য পাওয়া যায়নি</div>';
        outerCard.appendChild(emptyDiv);
        return;
      }
      var monthIdx = -1, netIdx = -1, grossIdx = -1, deductIdx = -1,
          statusIdx = -1, bonusIdx = -1, categoryIdx = -1, pfIdx = -1;
      headers.forEach(function(h, i) {
        if (h.includes('বেতনের মাস') || h.includes('মাস') || h.includes('উৎসবের নাম')) monthIdx = i;
        if (h.includes('নীট প্রদেয়'))     netIdx = i;
        if (h.includes('সর্বমোট'))         grossIdx = i;
        if (h.includes('মোট কর্তন'))       deductIdx = i;
        if (h.includes('অবস্থা') || h.toLowerCase().includes('status')) statusIdx = i;
        if (h.includes('বোনাস'))            bonusIdx = i;
        if (h.includes('ক্যাটাগরি') || h.toLowerCase().includes('category')) categoryIdx = i;
        if (h.includes('ভবিষ্যৎ তহবিল'))   pfIdx = i;
      });
      bodyRows.forEach(function(row) {
        var cells = Array.prototype.slice.call(row.cells);
        if (!cells.length) return;
        var firstText = ct(cells[0]);
        if (firstText.includes('মোট') && cells.length <= 2) {
          var summaryCard = document.createElement('div');
          summaryCard.className = 'bmu-list-row-card';
          summaryCard.style.cssText = '--blue: #059669';
          summaryCard.innerHTML = '<div class="bmu-list-row-primary"><span class="bmu-list-row-month">' + firstText + '</span>' + (cells[1] ? '<span class="bmu-list-row-net">৳ ' + ct(cells[1]) + '</span>' : '') + '</div>';
          outerCard.appendChild(summaryCard);
          return;
        }
        var rowCard = document.createElement('div');
        rowCard.className = 'bmu-list-row-card';
        var monthText  = monthIdx  >= 0 && cells[monthIdx]  ? ct(cells[monthIdx])  : '';
        var netText    = netIdx    >= 0 && cells[netIdx]    ? ct(cells[netIdx])    : '';
        var bonusText  = bonusIdx  >= 0 && cells[bonusIdx]  ? ct(cells[bonusIdx])  : '';
        var pfText     = pfIdx     >= 0 && cells[pfIdx]     ? ct(cells[pfIdx])     : '';
        var primaryRight = netText || bonusText || pfText;
        var primaryDiv = document.createElement('div');
        primaryDiv.className = 'bmu-list-row-primary';
        primaryDiv.innerHTML = '<span class="bmu-list-row-month">' + (monthText || '—') + '</span>' + (primaryRight ? '<span class="bmu-list-row-net">৳ ' + primaryRight + '</span>' : '');
        rowCard.appendChild(primaryDiv);
        var gridFields = [];
        if (grossIdx    >= 0 && cells[grossIdx])    gridFields.push({ label: 'সর্বমোট',   value: '৳ ' + ct(cells[grossIdx]),    cls: '' });
        if (deductIdx   >= 0 && cells[deductIdx])   gridFields.push({ label: 'মোট কর্তন', value: '৳ ' + ct(cells[deductIdx]),   cls: 'deduct' });
        if (categoryIdx >= 0 && cells[categoryIdx]) { var catText = ct(cells[categoryIdx]); if (catText && catText !== '\u00a0') gridFields.push({ label: 'ক্যাটাগরি', value: catText, cls: '' }); }
        if (statusIdx   >= 0 && cells[statusIdx])   { var stText = ct(cells[statusIdx]).trim(); if (stText && stText !== '\u00a0') gridFields.push({ label: 'অবস্থা', value: '<span class="bmu-status-badge ' + statusClass(stText) + '">' + stText + '</span>', cls: '', raw: true }); }
        if (gridFields.length) {
          var gridDiv = document.createElement('div');
          gridDiv.className = 'bmu-list-row-grid';
          gridFields.forEach(function(f) {
            var fieldDiv = document.createElement('div');
            fieldDiv.className = 'bmu-list-row-field';
            fieldDiv.innerHTML = '<span class="bmu-list-row-field-label">' + f.label + '</span>' + (f.raw ? f.value : '<span class="bmu-list-row-field-value ' + f.cls + '">' + f.value + '</span>');
            gridDiv.appendChild(fieldDiv);
          });
          rowCard.appendChild(gridDiv);
        }
        var foundAction = false;
        for (var ci = 0; ci < cells.length; ci++) {
          var aLink = cells[ci].querySelector('a[href]');
          if (aLink && aLink.href && !aLink.href.includes('#') && !aLink.href.endsWith('/')) {
            var actionBtn = document.createElement('a');
            actionBtn.className = 'bmu-list-row-action';
            actionBtn.href = aLink.href;
            actionBtn.textContent = 'বিস্তারিত দেখুন →';
            rowCard.appendChild(actionBtn);
            foundAction = true;
            break;
          }
        }
        if (!foundAction) {
          var onclick = row.getAttribute('onclick') || '';
          var idMatch = onclick.match(/DoNav\s*\(\s*['"]?(\d+)['"]?\s*\)/);
          if (idMatch) {
            var doNavBtn = document.createElement('a');
            doNavBtn.className = 'bmu-list-row-action';
            doNavBtn.href = 'javascript:DoNav(' + idMatch[1] + ')';
            doNavBtn.textContent = 'বিস্তারিত দেখুন →';
            rowCard.appendChild(doNavBtn);
          }
        }
        outerCard.appendChild(rowCard);
      });
    });

    var firstWrapper = listWrappers[0];
    if (firstWrapper && firstWrapper.parentNode) {
      firstWrapper.parentNode.insertBefore(outerCard, firstWrapper);
    } else {
      var viewBody = appArea.querySelector('.oe_view_manager_body');
      if (viewBody) viewBody.insertBefore(outerCard, viewBody.firstChild);
      else appArea.insertBefore(outerCard, appArea.firstChild);
    }
  }

  /* ════════════════════════════════════════════════════════════════
     6. PDS FORM PAGE OPTIMIZER
  ════════════════════════════════════════════════════════════════ */
  function optimizePdsFormPage() {
    var url = window.location.href;
    if (!url.includes('/pds/') && !url.includes('employee_basic')) return;
    if (document.querySelector('.bmu-pds-wrapped')) return;
    var formSheet = document.querySelector('.oe_form_sheetbg');
    if (!formSheet) return;
    Array.prototype.slice.call(formSheet.querySelectorAll('table.form_table')).forEach(function(tbl) {
      if (tbl.parentNode && !tbl.parentNode.classList.contains('bmu-form-card')) {
        var wrap = document.createElement('div');
        wrap.className = 'bmu-form-card';
        wrap.style.cssText = 'margin-bottom:12px;border-radius:16px;overflow:hidden;border:1px solid var(--border);box-shadow:0 1px 4px rgba(0,0,0,0.05);';
        tbl.parentNode.insertBefore(wrap, tbl);
        wrap.appendChild(tbl);
      }
    });
    Array.prototype.slice.call(formSheet.querySelectorAll('table.form_table td')).forEach(function(td) {
      td.removeAttribute('width');
      td.removeAttribute('height');
      if (td.getAttribute('colspan')) return;
      if (!td.classList.contains('oe_form_group_cell_label') &&
          !td.classList.contains('oe_form_group_cell')) return;
      td.removeAttribute('bgcolor');
    });
    formSheet.classList.add('bmu-pds-wrapped');
  }

  /* ════════════════════════════════════════════════════════════════
     7. LOGIN PAGE POLISH
  ════════════════════════════════════════════════════════════════ */
  function polishLoginPage() {
    var pane = document.querySelector('.oe_login_pane.oe_enterprise_pane');
    if (!pane) return;
    var form = pane.querySelector('form');
    if (!form || form.dataset.bmuPolished) return;
    form.dataset.bmuPolished = '1';
    var title = document.createElement('span');
    title.id = 'bmu-login-title'; title.textContent = 'BMU Portal';
    var sub = document.createElement('span');
    sub.id = 'bmu-login-subtitle'; sub.textContent = 'Human Resource Management System';
    form.insertBefore(sub, form.firstChild);
    form.insertBefore(title, form.firstChild);
    var uid  = form.querySelector('input[name="uid"]');
    var pass = form.querySelector('input[name="pass"]');
    if (uid)  uid.placeholder  = 'Enter your PDS ID';
    if (pass) pass.placeholder = 'Enter your password';
    var btn = form.querySelector('button[name="submit"]');
    if (btn) btn.textContent = 'Sign In';
  }

  /* ════════════════════════════════════════════════════════════════
     8. COLLECT NAV LINKS FROM SMARTMENU
  ════════════════════════════════════════════════════════════════ */
  function collectNavLinks() {
    var result = { homeItem: null, salaryItems: [], otherItems: [] };
    var smartmenu = document.querySelector('.smartmenu');
    if (!smartmenu) return result;
    Array.from(smartmenu.querySelectorAll('a')).forEach(function(a) {
      var href = a.getAttribute('href') || '';
      if (!href || href === '#') return;
      var text = a.textContent.trim();
      if (href.includes('inventory/home') || text.includes('হোম পেজ') || text.includes('হোম')) {
        if (!result.homeItem) result.homeItem = { text: text, href: href };
      } else if (href.includes('salary_ration_money')) {
        result.salaryItems.push({ text: text, href: href });
      } else {
        result.otherItems.push({ text: text, href: href });
      }
    });
    return result;
  }

  /* ════════════════════════════════════════════════════════════════
     9. DASHBOARD ICON GRID
  ════════════════════════════════════════════════════════════════ */
  function buildIconGrid() {
    if (document.getElementById('bmu-icon-grid')) return;
    var btnArea = document.querySelector('.oe_form_buttons');
    if (!btnArea) return;
    var links = Array.from(btnArea.querySelectorAll('a'));
    if (!links.length) return;

    var isHome = window.location.href.includes('home/index.php');

    if (isHome) document.body.classList.add('bmu-home');

    var logoutHref = 'https://pds.bmu.ac.bd/pds/user_mod/pages/main/logout.php';
    var topbarLogout = document.querySelector('.oe_topbar_item a[href*="logout"]');
    if (topbarLogout) logoutHref = topbarLogout.href;

    var matchers = [
      { test: function(h){ return h.includes('employee_basic'); }, icon:'📋', label:'পি ডি এস' },
      { test: function(h){ return h.includes('leave') && !h.includes('salary'); }, icon:'📅', label:null },
      { test: function(h){ return h.includes('attendance') || h.includes('Authenticate'); }, icon:'✅', label:'হাজিরা' }
    ];

    var coreTiles = [];
    var seenHrefs = {};
    links.forEach(function(a) {
      var href = a.href || '', text = a.textContent.trim();
      if (!href || seenHrefs[href]) return;
      seenHrefs[href] = true;
      var icon = '🔗', label = text;
      matchers.forEach(function(mt) { if (mt.test(href)) { icon = mt.icon; label = mt.label || text; } });
      coreTiles.push({ href: href, label: label, icon: icon });
    });

    var allTiles = [];
    if (isHome) {
      var nav = collectNavLinks();
      allTiles = allTiles.concat(coreTiles);
      if (nav.homeItem) {
        allTiles.push({ href: nav.homeItem.href, label: 'হোম পেজ', icon: '🏠' });
      }
      if (nav.salaryItems.length) {
        allTiles.push({ href: nav.salaryItems[0].href, label: 'বেতন ও ভাতাদি', icon: '💰' });
      }
      if (nav.otherItems.length) {
        allTiles.push({ href: nav.otherItems[0].href, label: 'আরও অপশন', icon: '⚙️' });
      }
    } else {
      allTiles = coreTiles;
    }

    allTiles.push({ href: logoutHref, label: 'লগ আউট', icon: '🚪' });

    var grid = document.createElement('div');
    grid.id = 'bmu-icon-grid';
    allTiles.forEach(function(t) {
      var a = document.createElement('a');
      a.className = 'bmu-tile'; a.href = t.href;
      a.innerHTML = '<span class="bmu-tile-icon">' + t.icon + '</span><span class="bmu-tile-label">' + t.label + '</span>';
      grid.appendChild(a);
    });
    btnArea.parentNode.insertBefore(grid, btnArea.nextSibling);
  }

  /* ════════════════════════════════════════════════════════════════
     10. SIDEBAR NAV
  ════════════════════════════════════════════════════════════════ */
  function transformMenu() {
    if (window.location.href.includes('home/index.php')) return;

    var smartmenu = document.querySelector('.smartmenu');
    if (!smartmenu || smartmenu.dataset.menuProcessed) return;
    smartmenu.dataset.menuProcessed = '1';

    var homeItem = null, salaryItems = [], otherItems = [];

    Array.from(smartmenu.querySelectorAll('a')).forEach(function(a) {
      var href = a.getAttribute('href') || '';
      if (!href || href === '#') return;
      var text = a.textContent.trim();
      if (href.includes('inventory/home') || text.includes('হোম পেজ') || text.includes('হোম')) {
        homeItem = { text: text, href: href };
      } else if (href.includes('salary_ration_money')) {
        salaryItems.push({ text: text, href: href });
      } else {
        otherItems.push({ text: text, href: href });
      }
    });

    smartmenu.innerHTML = '';

    if (homeItem) {
      var homeA = document.createElement('a');
      homeA.className = 'flat-menu-item';
      homeA.href = homeItem.href;
      homeA.textContent = homeItem.text;
      smartmenu.appendChild(homeA);
    }

    if (salaryItems.length) {
      var accordion = document.createElement('div');
      accordion.className = 'bmu-salary-accordion';
      var accHeader = document.createElement('div');
      accHeader.className = 'bmu-salary-accordion-header';
      accHeader.innerHTML = '<span>বেতন ও ভাতাদি</span><span class="accordion-arrow">▾</span>';
      var accBody = document.createElement('div');
      accBody.className = 'bmu-salary-accordion-body';
      salaryItems.forEach(function(item) {
        var a = document.createElement('a');
        a.className = 'bmu-salary-sub-item';
        a.href = item.href;
        a.textContent = item.text;
        accBody.appendChild(a);
      });
      accHeader.addEventListener('click', function(e) {
        e.preventDefault();
        var open = accHeader.classList.toggle('open');
        accBody.style.display = open ? 'block' : 'none';
      });
      accordion.appendChild(accHeader);
      accordion.appendChild(accBody);
      smartmenu.appendChild(accordion);
    }

    if (otherItems.length) {
      var container = document.createElement('div');
      container.className = 'custom-bottom-dropdown-container';
      var header = document.createElement('div');
      header.className = 'custom-dropdown-header';
      header.innerHTML = '<span>আরও অপশন</span><span class="dropdown-arrow">▾</span>';
      var content = document.createElement('div');
      content.className = 'custom-dropdown-content';
      otherItems.forEach(function(item) {
        var a = document.createElement('a');
        a.href = item.href; a.textContent = item.text;
        content.appendChild(a);
      });
      header.addEventListener('click', function(e) {
        e.preventDefault();
        var open = header.classList.toggle('open');
        content.style.display = open ? 'block' : 'none';
      });
      container.appendChild(header);
      container.appendChild(content);
      smartmenu.appendChild(container);
    }
  }

  /* ════════════════════════════════════════════════════════════════
     11. MOVE SIDEBAR BELOW USER CARD  (non-home pages only)
     The original DOM order inside the page is:
       td.oe_leftbar  (sidebar)
       td.oe_application
         └── table.oe_view_manager_header  (user/title card)
         └── … content …
     We pull td.oe_leftbar OUT of its original position and re-insert
     it INSIDE td.oe_application, immediately after the header table,
     so the visual order becomes: user card → sidebar nav → content.
  ════════════════════════════════════════════════════════════════ */
  function moveSidebarBelowUserCard() {
    if (window.location.href.includes('home/index.php')) return;
    if (document.getElementById('bmu-sidebar-moved')) return;

    var leftbar = document.querySelector('td.oe_leftbar');
    if (!leftbar) return;
    var appArea = document.querySelector('td.oe_application');
    if (!appArea) return;

    /* Mark so we don't run twice */
    leftbar.id = 'bmu-sidebar-moved';

    /* Style the leftbar so it fits inline inside appArea */
    leftbar.style.cssText = 'display:block!important;width:100%!important;padding:12px 12px 14px!important;background:var(--bg)!important;border-right:none!important;border-bottom:1px solid var(--border)!important;box-sizing:border-box!important;';

    /* Find the header table (user/title card) inside appArea */
    var headerTable = appArea.querySelector('table.oe_view_manager_header');

    if (headerTable) {
      /* Insert sidebar right after the header table */
      headerTable.parentNode.insertBefore(leftbar, headerTable.nextSibling);
    } else {
      /* Fallback: insert at the very top of appArea */
      appArea.insertBefore(leftbar, appArea.firstChild);
    }
  }

  /* ════════════════════════════════════════════════════════════════
     12. RUN ALL
  ════════════════════════════════════════════════════════════════ */
  function runAll() {
    polishLoginPage();
    transformMenu();
    moveSidebarBelowUserCard();
    buildIconGrid();
    rebuildSalaryPage();
    rebuildSalaryListPage();
    optimizePdsFormPage();
  }

  runAll();
  setTimeout(runAll, 400);
  setTimeout(runAll, 1200);

})();
""".trimIndent()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PortalScreen()
                }
            }
        }
    }
}

data class VersionInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val updateUrl: String
)

@Composable
fun GitHubUpdateChecker(context: Context) {
    var showDialog by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("") }
    var latestVersionName by remember { mutableStateOf("") }

    val currentVersionCode = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    LaunchedEffect(Unit) {
        val jsonUrl = "https://raw.githubusercontent.com/HSaikat/BMUPDS/master/version.json"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(jsonUrl)
            .header("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) return
                response.body?.string()?.let { jsonString ->
                    try {
                        val versionInfo = Gson().fromJson(jsonString, VersionInfo::class.java)
                        if (versionInfo.latestVersionCode > currentVersionCode) {
                            updateUrl = versionInfo.updateUrl
                            latestVersionName = versionInfo.latestVersionName
                            showDialog = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(text = "New Update Available!") },
            text = { Text(text = "Version $latestVersionName is available. Please update the app.") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                    context.startActivity(intent)
                }) {
                    Text("Update Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Later")
                }
            }
        )
    }
}

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalScreen() {
    val context      = androidx.compose.ui.platform.LocalContext.current
    var isLoading    by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var canGoBack    by remember { mutableStateOf(false) }
    var isOffline    by remember { mutableStateOf<Boolean?>(null) }
    val pullState    = rememberPullToRefreshState()
    val scope        = rememberCoroutineScope()
    var webViewRef: WebView? by remember { mutableStateOf(null) }

    GitHubUpdateChecker(context)

    LaunchedEffect(Unit) { isOffline = !isOnline(context) }
    BackHandler(enabled = canGoBack) { webViewRef?.goBack() }

    if (isOffline == null) return

    if (isOffline == true) {
        NoInternetScreen(isRefreshing = isRefreshing, onRetry = {
            scope.launch {
                isRefreshing = true
                if (isOnline(context)) {
                    isOffline = false
                    isLoading = true
                    webViewRef?.loadUrl(PORTAL_URL)
                }
                isRefreshing = false
            }
        })
        return
    }

    PullToRefreshBox(
        state        = pullState,
        isRefreshing = isRefreshing,
        onRefresh    = {
            scope.launch {
                if (!isOnline(context)) { isOffline = true; isRefreshing = false }
                else { isRefreshing = true; webViewRef?.reload() }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory  = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            @Suppress("DEPRECATION") javaScriptEnabled = true
                            domStorageEnabled    = true
                            databaseEnabled      = true
                            useWideViewPort      = true
                            loadWithOverviewMode = true
                            cacheMode            = WebSettings.LOAD_DEFAULT
                            mixedContentMode     = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            @Suppress("DEPRECATION") setSupportZoom(false)
                            builtInZoomControls  = false
                            displayZoomControls  = false
                        }
                        webViewClient = object : WebViewClient() {
                            @SuppressLint("WebViewClientOnReceivedSslError")
                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                handler?.proceed()
                            }
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true; isOffline = false
                                canGoBack = view?.canGoBack() ?: false
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                view?.evaluateJavascript(INJECT_JS, null)
                                isLoading = false; isRefreshing = false
                                canGoBack = view?.canGoBack() ?: false
                            }
                            @Suppress("DEPRECATION")
                            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                view?.post {
                                    if (!isOnline(context)) {
                                        isOffline = true; isLoading = false; isRefreshing = false
                                    }
                                }
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                view?.loadUrl(request?.url.toString())
                                canGoBack = view?.canGoBack() ?: false
                                return true
                            }
                        }
                        loadUrl(PORTAL_URL)
                        webViewRef = this
                    }
                },
                update = { wv -> webViewRef = wv }
            )
            if (isLoading && !isRefreshing) {
                CircularProgressIndicator(
                    modifier    = Modifier.align(Alignment.Center).padding(16.dp),
                    color       = Color(0xFF2563EB),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoInternetScreen(isRefreshing: Boolean, onRetry: () -> Unit) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullState, isRefreshing = isRefreshing,
        onRefresh = onRetry, modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF0A0F1E), Color(0xFF0F172A), Color(0xFF1A1F35)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(40.dp)
            ) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(
                        Brush.linearGradient(listOf(Color(0xFF1E3A5F), Color(0xFF1E40AF)))
                    ),
                    contentAlignment = Alignment.Center
                ) { Text("📡", fontSize = 40.sp) }
                Spacer(Modifier.height(28.dp))
                Text(
                    "No Internet Connection",
                    color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Please check your Wi-Fi or Mobile Data\nand try again.",
                    color = Color(0xFF94A3B8), fontSize = 14.sp,
                    textAlign = TextAlign.Center, lineHeight = 22.sp
                )
                Spacer(Modifier.height(36.dp))
                Button(
                    onClick = onRetry, enabled = !isRefreshing,
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.height(52.dp).padding(horizontal = 8.dp)
                ) {
                    if (isRefreshing)
                        CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                    else
                        Text("Try Again", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}