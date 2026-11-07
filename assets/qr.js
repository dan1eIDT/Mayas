/* Copyright (C) 2026 dan1eIDT */
(function () {
  function build(text, options) {
    const opts = Object.assign({
      level: 'H',
      margin: 4,
      dark: '#0b0b0e',
      light: '#ffffff',
      logo: null,
      logoScale: 0.2,
      label: 'QR'
    }, options);

    const qr = qrcode(0, opts.level);
    qr.addData(text);
    qr.make();

    const n = qr.getModuleCount();
    const size = n + opts.margin * 2;
    let path = '';
    for (let r = 0; r < n; r++) {
      let c = 0;
      while (c < n) {
        if (qr.isDark(r, c)) {
          const start = c;
          while (c < n && qr.isDark(r, c)) c++;
          path += 'M' + (start + opts.margin) + ' ' + (r + opts.margin) + 'h' + (c - start) + 'v1h-' + (c - start) + 'z';
        } else {
          c++;
        }
      }
    }

    let logo = '';
    if (opts.logo) {
      const logoSize = Math.round(n * opts.logoScale);
      const pad = 1.2;
      const box = logoSize + pad * 2;
      const x = (size - box) / 2;
      logo =
        '<rect x="' + x + '" y="' + x + '" width="' + box + '" height="' + box + '" rx="' + (box * 0.22) + '" fill="' + opts.light + '"/>' +
        '<image href="' + opts.logo + '" x="' + (x + pad) + '" y="' + (x + pad) + '" width="' + logoSize + '" height="' + logoSize + '" preserveAspectRatio="xMidYMid slice"/>';
    }

    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ' + size + ' ' + size + '" role="img" aria-label="' + opts.label + '" shape-rendering="crispEdges">' +
      '<rect width="' + size + '" height="' + size + '" fill="' + opts.light + '"/>' +
      '<path d="' + path + '" fill="' + opts.dark + '"/>' +
      logo +
      '</svg>';
  }

  function render(el, text, options) {
    el.innerHTML = build(text, options);
  }

  window.MayasQR = { build: build, render: render };
})();
