#!/usr/bin/env python3
# Patches the decoded app (apktool dir) to add tablet modes:
#   - toolbar toggle: SPREAD (two columns) vs OUTLINE (side panel + reader)
#   - OUTLINE side panel mirrors the origin: Home screen if opened from Home,
#     Table of Contents if opened from the TOC.
# All new logic is in self-contained methods, every call site is try/catch(Throwable)
# wrapped, so any failure falls back to normal behavior instead of crashing.
import io, os, sys

base = sys.argv[1]
pf = os.path.join(base, "smali_classes6/org/chabad/kehossiddur/PrayerFragment.smali")
ma = os.path.join(base, "smali_classes6/org/chabad/kehossiddur/MainActivity.smali")

# ============================ PrayerFragment.smali ============================
s = io.open(pf, encoding="utf-8").read()

anchor_open = ("    invoke-virtual {p2, p1, v0, v1, v2}, Lorg/chabad/kehossiddur/PDFView;"
               "->openFile(Ljava/lang/String;Ljava/util/ArrayList;II)Lru/mobigroup/bookreader/MuPDFCore;\n")
call_setup = (
    "\n    :try_start_tab\n"
    "    invoke-direct {p0, p1, v0, v1, v2}, Lorg/chabad/kehossiddur/PrayerFragment;->tabletSetup(Ljava/lang/String;Ljava/util/ArrayList;II)V\n"
    "    :try_end_tab\n"
    "    .catch Ljava/lang/Throwable; {:try_start_tab .. :try_end_tab} :catch_tab\n"
    "    goto :after_tab\n    :catch_tab\n    move-exception v3\n    :after_tab\n")
assert s.count(anchor_open) == 1, "openFile anchor"
s = s.replace(anchor_open, anchor_open + call_setup, 1)

anchor_inflate = "    invoke-virtual {p2, v0, p1}, Landroid/view/MenuInflater;->inflate(ILandroid/view/Menu;)V\n"
call_menu = (
    "\n    :try_start_tmenu\n"
    "    invoke-direct {p0, p1}, Lorg/chabad/kehossiddur/PrayerFragment;->tabletMenu(Landroid/view/Menu;)V\n"
    "    :try_end_tmenu\n"
    "    .catch Ljava/lang/Throwable; {:try_start_tmenu .. :try_end_tmenu} :catch_tmenu\n"
    "    goto :after_tmenu\n    :catch_tmenu\n    move-exception v0\n    :after_tmenu\n")
assert s.count(anchor_inflate) == 1, "inflate anchor"
s = s.replace(anchor_inflate, anchor_inflate + call_menu, 1)

anchor_sel = (".method public onOptionsItemSelected(Landroid/view/MenuItem;)Z\n    .locals 13\n\n"
              "    const-string v0, \"item\"\n\n"
              "    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V\n")
guard_sel = (
    "\n    const/4 v0, 0x0\n    :try_start_tsel\n"
    "    invoke-direct {p0, p1}, Lorg/chabad/kehossiddur/PrayerFragment;->tabletHandleToggle(Landroid/view/MenuItem;)Z\n"
    "    move-result v0\n    :try_end_tsel\n"
    "    .catch Ljava/lang/Throwable; {:try_start_tsel .. :try_end_tsel} :catch_tsel\n"
    "    goto :after_tsel\n    :catch_tsel\n    move-exception v1\n    const/4 v0, 0x0\n    :after_tsel\n"
    "    if-eqz v0, :orig_sel\n    const/4 v0, 0x1\n    return v0\n    :orig_sel\n")
assert s.count(anchor_sel) == 1, "onOptionsItemSelected anchor"
s = s.replace(anchor_sel, anchor_sel + guard_sel, 1)

methods = r'''
.method private tabletSetup(Ljava/lang/String;Ljava/util/ArrayList;II)V
    .locals 11
    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->requireContext()Landroid/content/Context;
    move-result-object v0
    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->requireView()Landroid/view/View;
    move-result-object v1
    invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
    move-result-object v2
    invoke-virtual {v0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
    move-result-object v3
    const-string v4, "id"
    const-string v5, "pdfview_right"
    invoke-virtual {v2, v5, v4, v3}, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
    move-result v5
    invoke-virtual {v1, v5}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v5
    const-string v6, "toc_side_panel"
    invoke-virtual {v2, v6, v4, v3}, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
    move-result v6
    invoke-virtual {v1, v6}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v7
    invoke-static {v0}, Landroidx/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v8
    const-string v9, "tabletLayout"
    const-string v10, "spread"
    invoke-interface {v8, v9, v10}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v8
    const-string v9, "outline"
    invoke-virtual {v8, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v8
    if-eqz v8, :spread
    if-eqz v7, :done
    const/4 v9, 0x0
    invoke-virtual {v7, v9}, Landroid/view/View;->setVisibility(I)V
    if-eqz v5, :hidedone
    const/16 v9, 0x8
    invoke-virtual {v5, v9}, Landroid/view/View;->setVisibility(I)V
    :hidedone
    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->getChildFragmentManager()Landroidx/fragment/app/FragmentManager;
    move-result-object v9
    invoke-virtual {v9, v6}, Landroidx/fragment/app/FragmentManager;->findFragmentById(I)Landroidx/fragment/app/Fragment;
    move-result-object v10
    if-nez v10, :done
    invoke-virtual {v9}, Landroidx/fragment/app/FragmentManager;->beginTransaction()Landroidx/fragment/app/FragmentTransaction;
    move-result-object v10
    invoke-static {v0}, Landroidx/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v1
    const-string v2, "prayerOrigin"
    const/4 v3, 0x1
    invoke-interface {v1, v2, v3}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v1
    const/4 v3, 0x2
    if-eq v1, v3, :mktoc
    new-instance v4, Lorg/chabad/kehossiddur/HomeScreenFragment;
    invoke-direct {v4}, Lorg/chabad/kehossiddur/HomeScreenFragment;-><init>()V
    goto :mkdone
    :mktoc
    new-instance v4, Lorg/chabad/kehossiddur/TOCFragment;
    invoke-direct {v4}, Lorg/chabad/kehossiddur/TOCFragment;-><init>()V
    :mkdone
    invoke-virtual {v10, v6, v4}, Landroidx/fragment/app/FragmentTransaction;->replace(ILandroidx/fragment/app/Fragment;)Landroidx/fragment/app/FragmentTransaction;
    move-result-object v10
    invoke-virtual {v10}, Landroidx/fragment/app/FragmentTransaction;->commitAllowingStateLoss()I
    goto :done
    :spread
    if-eqz v5, :done
    instance-of v9, v5, Lorg/chabad/kehossiddur/PDFView;
    if-eqz v9, :done
    if-eqz v7, :spreadshow
    const/16 v9, 0x8
    invoke-virtual {v7, v9}, Landroid/view/View;->setVisibility(I)V
    :spreadshow
    const/4 v9, 0x0
    invoke-virtual {v5, v9}, Landroid/view/View;->setVisibility(I)V
    check-cast v5, Lorg/chabad/kehossiddur/PDFView;
    add-int/lit8 v9, p3, 0x1
    invoke-virtual {v5, p1, p2, v9, p4}, Lorg/chabad/kehossiddur/PDFView;->openFile(Ljava/lang/String;Ljava/util/ArrayList;II)Lru/mobigroup/bookreader/MuPDFCore;
    invoke-virtual {v5}, Lorg/chabad/kehossiddur/PDFView;->checkHasSizes()V
    :done
    return-void
.end method

.method private tabletMenu(Landroid/view/Menu;)V
    .locals 6
    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->requireContext()Landroid/content/Context;
    move-result-object v0
    invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
    move-result-object v1
    invoke-virtual {v1}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;
    move-result-object v1
    iget v1, v1, Landroid/content/res/Configuration;->smallestScreenWidthDp:I
    const/16 v2, 0x258
    if-lt v1, v2, :mdone
    invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
    move-result-object v1
    invoke-virtual {v0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
    move-result-object v2
    const-string v3, "tablet_view_toggle"
    const-string v4, "id"
    invoke-virtual {v1, v3, v4, v2}, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
    move-result v1
    invoke-interface {p1, v1}, Landroid/view/Menu;->findItem(I)Landroid/view/MenuItem;
    move-result-object v1
    if-eqz v1, :mdone
    const/4 v2, 0x1
    invoke-interface {v1, v2}, Landroid/view/MenuItem;->setVisible(Z)Landroid/view/MenuItem;
    :mdone
    return-void
.end method

.method private tabletHandleToggle(Landroid/view/MenuItem;)Z
    .locals 8
    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->requireContext()Landroid/content/Context;
    move-result-object v0
    invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
    move-result-object v1
    invoke-virtual {v0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
    move-result-object v2
    const-string v3, "tablet_view_toggle"
    const-string v4, "id"
    invoke-virtual {v1, v3, v4, v2}, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
    move-result v1
    invoke-interface {p1}, Landroid/view/MenuItem;->getItemId()I
    move-result v2
    if-eq v1, v2, :istoggle
    const/4 v0, 0x0
    return v0
    :istoggle
    invoke-static {v0}, Landroidx/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v1
    const-string v2, "tabletLayout"
    const-string v3, "spread"
    invoke-interface {v1, v2, v3}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v3
    const-string v4, "outline"
    invoke-virtual {v3, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v3
    if-eqz v3, :setoutline
    const-string v4, "spread"
    goto :doset
    :setoutline
    const-string v4, "outline"
    :doset
    invoke-interface {v1}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;
    move-result-object v5
    invoke-interface {v5, v2, v4}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;
    move-result-object v5
    invoke-interface {v5}, Landroid/content/SharedPreferences$Editor;->apply()V
    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->requireActivity()Landroidx/fragment/app/FragmentActivity;
    move-result-object v5
    check-cast v5, Lorg/chabad/kehossiddur/MainActivity;
    const/16 v6, 0x8
    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v6
    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->requireArguments()Landroid/os/Bundle;
    move-result-object v7
    invoke-virtual {v5, v6, v7}, Lorg/chabad/kehossiddur/MainActivity;->setFragment(Ljava/lang/Integer;Landroid/os/Bundle;)V
    const/4 v0, 0x1
    return v0
.end method
'''
s = s.rstrip() + "\n" + methods
io.open(pf, "w", encoding="utf-8").write(s)
print("PrayerFragment.smali patched OK")

# ============================ MainActivity.smali ============================
m = io.open(ma, encoding="utf-8").read()
anchor_ot = ("    const-string v0, \"overrideTitle\"\n\n"
             "    invoke-static {p7, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V\n")
capture = (
    "\n    :try_start_orig\n"
    "    iget-object v0, p0, Lorg/chabad/kehossiddur/MainActivity;->lastFragment:Ljava/lang/Integer;\n"
    "    if-eqz v0, :orig_done\n"
    "    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I\n"
    "    move-result v0\n"
    "    const/16 v1, 0x8\n"
    "    if-eq v0, v1, :orig_done\n"
    "    invoke-static {p0}, Landroidx/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;\n"
    "    move-result-object v1\n"
    "    invoke-interface {v1}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;\n"
    "    move-result-object v1\n"
    "    const-string v2, \"prayerOrigin\"\n"
    "    invoke-interface {v1, v2, v0}, Landroid/content/SharedPreferences$Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences$Editor;\n"
    "    move-result-object v1\n"
    "    invoke-interface {v1}, Landroid/content/SharedPreferences$Editor;->apply()V\n"
    "    :orig_done\n"
    "    :try_end_orig\n"
    "    .catch Ljava/lang/Throwable; {:try_start_orig .. :try_end_orig} :catch_orig\n"
    "    goto :after_orig\n    :catch_orig\n    move-exception v0\n    :after_orig\n")
assert m.count(anchor_ot) == 1, "overrideTitle anchor not unique/found: %d" % m.count(anchor_ot)
m = m.replace(anchor_ot, anchor_ot + capture, 1)
io.open(ma, "w", encoding="utf-8").write(m)
print("MainActivity.smali patched OK")
