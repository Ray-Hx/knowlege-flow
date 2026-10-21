(() => {
  'use strict';

  const source = document.getElementById('guide-source').textContent.trim();
  const content = document.getElementById('guide-content');
  const chapterList = document.getElementById('chapter-list');
  const searchInput = document.getElementById('search-input');
  const searchResults = document.getElementById('search-results');
  const searchWrap = document.getElementById('search-wrap');
  const themeToggle = document.getElementById('theme-toggle');
  const progressBar = document.getElementById('reading-progress');
  const navProgress = document.getElementById('nav-progress');
  const progressLabel = document.getElementById('progress-label');
  const currentSection = document.getElementById('current-section');
  const chapterNav = document.getElementById('chapter-nav');
  const menuOpen = document.getElementById('menu-open');
  const menuClose = document.getElementById('menu-close');
  const navBackdrop = document.getElementById('nav-backdrop');
  const copyToast = document.getElementById('copy-toast');
  let headingObserver;
  let searchIndex = [];
  let mermaidCharts = [];

  function escapeHtml(value) {
    return String(value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

  function plainText(value) {
    const temp = document.createElement('div');
    temp.innerHTML = String(value)
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
      .replace(/[`*_>#|~-]/g, ' ');
    return (temp.textContent || '')
      .replace(/\s+/g, ' ')
      .trim();
  }

  function slugify(value) {
    return (
      plainText(value)
        .toLowerCase()
        .replace(/[\s/]+/g, '-')
        .replace(/[^\p{Letter}\p{Number}-]/gu, '')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '') || 'section'
    );
  }

  function uniqueSlug(base) {
    let id = base;
    let index = 2;
    while (document.getElementById(id)) {
      id = `${base}-${index}`;
      index += 1;
    }
    return id;
  }

  function configureMarkdown() {
    const renderer = new marked.Renderer();

    renderer.heading = function ({ tokens, depth }) {
      const label = this.parser.parseInline(tokens);
      const id = uniqueSlug(slugify(label));
      return `<h${depth} id="${id}">${label}</h${depth}>`;
    };

    renderer.code = function ({ text, lang }) {
      const language = (lang || '').trim().split(/\s+/)[0];
      if (language === 'mermaid') {
        return `<div class="mermaid" data-chart="${encodeURIComponent(text)}">${escapeHtml(text)}</div>`;
      }
      return `<pre class="code-block"><span class="code-language">${escapeHtml(language || 'code')}</span><code>${escapeHtml(text)}</code></pre>`;
    };

    renderer.link = function ({ href, title, tokens }) {
      const label = this.parser.parseInline(tokens);
      const local = /^(src\/|docs\/|pom\.xml$|docker-compose\.yml$)/.test(href);
      const resolved = local ? `../${href}` : href;
      const attrs = [
        `href="${escapeHtml(resolved)}"`,
        title ? `title="${escapeHtml(title)}"` : '',
        local ? 'class="source-link"' : '',
        local ? `data-source-path="${escapeHtml(href)}"` : '',
        /^https?:\/\//.test(href) ? 'target="_blank" rel="noreferrer"' : '',
      ]
        .filter(Boolean)
        .join(' ');
      return `<a ${attrs}>${local ? '<span aria-hidden="true">↗</span>' : ''}${label}</a>`;
    };

    marked.setOptions({ gfm: true, breaks: false, renderer });
  }

  function renderMarkdown() {
    configureMarkdown();
    content.innerHTML = marked.parse(source);

    content.querySelectorAll('table').forEach((table) => {
      if (table.parentElement?.classList.contains('table-shell')) return;
      const shell = document.createElement('div');
      shell.className = 'table-shell';
      table.parentNode.insertBefore(shell, table);
      shell.appendChild(table);
    });

    content.querySelectorAll('input[type="checkbox"]').forEach((checkbox) => {
      checkbox.disabled = false;
      const key = `kflow-check-${plainText(checkbox.parentElement?.textContent || '')}`;
      checkbox.checked = localStorage.getItem(key) === '1';
      checkbox.addEventListener('change', () => {
        localStorage.setItem(key, checkbox.checked ? '1' : '0');
      });
    });

    mermaidCharts = Array.from(content.querySelectorAll('.mermaid')).map((node) => ({
      node,
      chart: decodeURIComponent(node.dataset.chart || ''),
    }));
  }

  function isDark() {
    return document.documentElement.classList.contains('dark');
  }

  async function renderMermaid() {
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'loose',
      theme: isDark() ? 'dark' : 'base',
      fontFamily: 'Inter, PingFang SC, Microsoft YaHei, sans-serif',
      themeVariables: isDark()
        ? {
            primaryColor: '#16354a',
            primaryTextColor: '#e8f4f7',
            primaryBorderColor: '#4db7c5',
            lineColor: '#7f9eaa',
            secondaryColor: '#2a251c',
            tertiaryColor: '#111d24',
          }
        : {
            primaryColor: '#e7f5f5',
            primaryTextColor: '#16313a',
            primaryBorderColor: '#178997',
            lineColor: '#647b82',
            secondaryColor: '#fff3d6',
            tertiaryColor: '#f5faf9',
          },
    });

    for (let index = 0; index < mermaidCharts.length; index += 1) {
      const { node, chart } = mermaidCharts[index];
      node.removeAttribute('data-processed');
      node.classList.remove('diagram-error');
      node.textContent = chart;
      try {
        const rendered = await mermaid.render(`local-mermaid-${Date.now()}-${index}`, chart);
        node.innerHTML = rendered.svg;
        rendered.bindFunctions?.(node);
      } catch (error) {
        node.textContent = chart;
        node.classList.add('diagram-error');
      }
    }
  }

  function buildNavigation() {
    const headings = Array.from(content.querySelectorAll('h2'));
    chapterList.innerHTML = '';

    headings.forEach((heading, index) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.dataset.target = heading.id;
      button.innerHTML = `
        <span>${String(index).padStart(2, '0')}</span>
        <b>${escapeHtml(heading.textContent)}</b>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
      `;
      button.addEventListener('click', () => jumpTo(heading.id));
      chapterList.appendChild(button);
    });

    headingObserver?.disconnect();
    headingObserver = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (visible[0]) setActiveHeading(visible[0].target.id);
      },
      { rootMargin: '-18% 0px -72% 0px' },
    );
    headings.forEach((heading) => headingObserver.observe(heading));
  }

  function setActiveHeading(id) {
    chapterList.querySelectorAll('button').forEach((button) => {
      button.classList.toggle('active', button.dataset.target === id);
    });
    const heading = document.getElementById(id);
    if (heading) currentSection.textContent = heading.textContent;
  }

  function jumpTo(id) {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    history.replaceState(null, '', `#${encodeURIComponent(id)}`);
    searchInput.value = '';
    searchResults.hidden = true;
    closeMenu();
  }

  function buildSearchIndex() {
    searchIndex = [];
    let section = 'Learning Map';
    let sectionId = content.querySelector('h2')?.id || 'learning-map';

    content.querySelectorAll('h2, h3, p, li').forEach((node) => {
      if (node.matches('h2, h3')) {
        section = node.textContent.trim();
        sectionId = node.id;
      }
      const text = plainText(node.textContent);
      if (text.length >= 12) searchIndex.push({ id: sectionId, section, text });
    });
  }

  function markedText(text, query) {
    const index = text.toLowerCase().indexOf(query.toLowerCase());
    if (index < 0) return escapeHtml(text);
    return `${escapeHtml(text.slice(0, index))}<mark>${escapeHtml(
      text.slice(index, index + query.length),
    )}</mark>${escapeHtml(text.slice(index + query.length))}`;
  }

  function runSearch() {
    const query = searchInput.value.trim();
    if (query.length < 2) {
      searchResults.hidden = true;
      return;
    }

    const normalized = query.toLowerCase();
    const matches = searchIndex
      .filter(
        (entry) =>
          entry.text.toLowerCase().includes(normalized) ||
          entry.section.toLowerCase().includes(normalized),
      )
      .slice(0, 8);

    searchResults.innerHTML = '';
    if (!matches.length) {
      searchResults.innerHTML = '<p class="search-empty">没有找到相关内容</p>';
    } else {
      matches.forEach((entry) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.innerHTML = `<span>${escapeHtml(entry.section)}</span><p>${markedText(
          entry.text,
          query,
        )}</p>`;
        button.addEventListener('click', () => jumpTo(entry.id));
        searchResults.appendChild(button);
      });
    }
    searchResults.hidden = false;
  }

  function updateProgress() {
    const max = document.documentElement.scrollHeight - window.innerHeight;
    const progress = max > 0 ? Math.min(100, (window.scrollY / max) * 100) : 0;
    const value = `${progress}%`;
    progressBar.style.width = value;
    navProgress.style.width = value;
    progressLabel.textContent = `${Math.round(progress)}%`;
  }

  function openMenu() {
    chapterNav.classList.add('is-open');
    navBackdrop.hidden = false;
    document.body.classList.add('menu-open');
  }

  function closeMenu() {
    chapterNav.classList.remove('is-open');
    navBackdrop.hidden = true;
    document.body.classList.remove('menu-open');
  }

  function initializeTheme() {
    const stored = localStorage.getItem('kflow-guide-theme');
    const dark =
      stored === 'dark' ||
      (!stored && window.matchMedia('(prefers-color-scheme: dark)').matches);
    document.documentElement.classList.toggle('dark', dark);
  }

  function bindEvents() {
    searchInput.addEventListener('input', runSearch);
    searchInput.addEventListener('search', runSearch);
    document.addEventListener('click', (event) => {
      if (!searchWrap.contains(event.target)) searchResults.hidden = true;
    });

    themeToggle.addEventListener('click', async () => {
      document.documentElement.classList.toggle('dark');
      localStorage.setItem('kflow-guide-theme', isDark() ? 'dark' : 'light');
      await renderMermaid();
    });

    menuOpen.addEventListener('click', openMenu);
    menuClose.addEventListener('click', closeMenu);
    navBackdrop.addEventListener('click', closeMenu);

    window.addEventListener('scroll', updateProgress, { passive: true });
    window.addEventListener('keydown', (event) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        searchInput.focus();
      }
      if (event.key === 'Escape') {
        searchInput.value = '';
        searchResults.hidden = true;
        closeMenu();
      }
    });

    content.addEventListener('click', async (event) => {
      const link = event.target.closest('.source-link');
      if (!link || !event.altKey) return;
      event.preventDefault();
      await navigator.clipboard.writeText(link.dataset.sourcePath || link.getAttribute('href'));
      copyToast.hidden = false;
      window.setTimeout(() => {
        copyToast.hidden = true;
      }, 1600);
    });
  }

  async function initialize() {
    initializeTheme();
    renderMarkdown();
    buildNavigation();
    buildSearchIndex();
    bindEvents();
    updateProgress();
    await renderMermaid();

    if (location.hash) {
      const id = decodeURIComponent(location.hash.slice(1));
      window.setTimeout(() => document.getElementById(id)?.scrollIntoView(), 50);
    }
  }

  initialize().catch((error) => {
    content.innerHTML = `
      <div class="fatal-error">
        <strong>本地页面加载失败</strong>
        <p>${escapeHtml(error?.message || String(error))}</p>
      </div>
    `;
  });
})();
