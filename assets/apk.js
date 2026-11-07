/* Copyright (C) 2026 dan1eIDT */
(function () {
  const API = 'https://api.github.com/repos/dan1eIDT/Mayas/releases?per_page=5';
  const CACHE_KEY = 'mayas-releases-v1';
  const TTL = 10 * 60 * 1000;
  let inflight = null;

  function readCache() {
    try {
      const raw = sessionStorage.getItem(CACHE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (_) {
      return null;
    }
  }

  function writeCache(data) {
    try {
      sessionStorage.setItem(CACHE_KEY, JSON.stringify({ t: Date.now(), data }));
    } catch (_) {}
  }

  function load() {
    const cached = readCache();
    if (cached && Date.now() - cached.t < TTL) return Promise.resolve(cached.data);
    if (inflight) return inflight;
    inflight = fetch(API, { headers: { Accept: 'application/vnd.github+json' } })
      .then((res) => {
        if (!res.ok) throw new Error('GitHub API ' + res.status);
        return res.json();
      })
      .then((data) => {
        if (!Array.isArray(data) || data.length === 0) throw new Error('No releases');
        writeCache(data);
        return data;
      })
      .catch((err) => {
        if (cached) return cached.data;
        throw err;
      })
      .finally(() => {
        inflight = null;
      });
    return inflight;
  }

  function latestStable(releases) {
    return releases.find((r) => !r.draft && !r.prerelease) || releases[0];
  }

  function pickApk(release) {
    const apks = ((release && release.assets) || []).filter((a) => /\.apk$/i.test(a.name));
    if (apks.length === 0) return null;
    return (
      apks.find((a) => /universal/i.test(a.name)) ||
      apks.find((a) => !/debug/i.test(a.name)) ||
      apks[0]
    );
  }

  function applyDownloadLinks(release) {
    const apk = pickApk(release);
    if (!apk) return null;
    document.querySelectorAll('[data-apk]').forEach((el) => {
      el.setAttribute('href', apk.browser_download_url);
      el.removeAttribute('target');
      el.setAttribute('rel', 'noopener');
    });
    const mb = (apk.size / 1048576).toFixed(1);
    document.querySelectorAll('[data-apk-meta]').forEach((el) => {
      el.textContent = release.tag_name + ' · ' + mb + ' MB · APK';
      el.hidden = false;
    });
    return apk;
  }

  window.MayasReleases = { load, latestStable, pickApk, applyDownloadLinks };
})();
