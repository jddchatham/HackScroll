const SORT_DATE = 0;
const SORT_TOP = 1;
const SORT_RND = 2;
const PAGE_LIMIT = 100;

const state = {
  articles: [],
  loadedLinks: new Set(),
  searchResults: [],
  searchLinks: new Set(),
  visible: [],
  pageRefs: [],
  sortMode: SORT_DATE,
  searchQuery: '',
  loading: false,
  renderedOnce: false,
  noMoreBrowse: false,
  noMoreSearch: false,
  nextBrowsePage: 1,
  nextSearchPage: 1,
  shuffled: null,
  shuffledBase: null,
  pendingStartPage: -1,
  currentPage: 0,
  firstPageEvent: true,
  hintShown: localStorage.getItem('hackscroll-hint') === '1'
};

const els = {
  pager: document.getElementById('pager'),
  topbar: document.getElementById('topbar'),
  sortDate: document.getElementById('sort-date'),
  sortTop: document.getElementById('sort-top'),
  sortRnd: document.getElementById('sort-rnd'),
  refresh: document.getElementById('refresh'),
  searchToggle: document.getElementById('search-toggle'),
  searchExpanded: document.getElementById('search-expanded'),
  searchInput: document.getElementById('search-input'),
  searchGo: document.getElementById('search-go'),
  searchClear: document.getElementById('search-clear'),
  matchInfo: document.getElementById('match-info'),
  scrollUp: document.getElementById('scroll-up'),
  scrollDown: document.getElementById('scroll-down'),
  offline: document.getElementById('offline'),
  loading: document.getElementById('loading'),
  error: document.getElementById('error'),
  errorBtn: document.getElementById('error-btn'),
  hint: document.getElementById('hint')
};

function stripTags(html) {
  return (html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ');
}

function decodeEntities(str) {
  if (!str) return '';
  const txt = document.createElement('textarea');
  txt.innerHTML = str;
  return txt.value;
}

function makeExcerpt(html) {
  let text = decodeEntities(stripTags(html || '')).trim();
  const idx = text.toLowerCase().indexOf('read more');
  if (idx > 0) text = text.slice(0, idx);
  text = text.replace(/[\s…]+$/g, '').trim();
  if (text.length > 240) {
    let sub = text.slice(0, 240);
    const last = sub.lastIndexOf(' ');
    if (last > 120) sub = sub.slice(0, last);
    return sub + '…';
  }
  return text;
}

function parseDate(str) {
  if (!str) return 0;
  const t = Date.parse(str.trim());
  return isNaN(t) ? 0 : t;
}

function niceDate(str) {
  if (!str) return '';
  const t = parseDate(str);
  if (!t) return str;
  return new Date(t).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

function firstImage(html) {
  if (!html) return '';
  const m = html.match(/<img[^>]+?src=["']([^"']+)["']/i);
  return m ? m[1].trim() : '';
}

function findWpImage(post) {
  try {
    const emb = post._embedded;
    if (!emb) return '';
    const media = emb['wp:featuredmedia'];
    if (!media || !media.length) return '';
    const m = media[0];
    const sizes = m.media_details && m.media_details.sizes;
    if (sizes && sizes.medium && sizes.medium.source_url) return sizes.medium.source_url;
    return m.source_url || '';
  } catch (e) {
    return '';
  }
}

function parseRss(xmlText) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(xmlText, 'application/xml');
  const items = doc.querySelectorAll('item');
  const out = [];
  items.forEach(item => {
    const title = item.querySelector('title')?.textContent || '';
    const link = item.querySelector('link')?.textContent?.trim() || '';
    if (!title || !link) return;
    const pubDate = item.querySelector('pubDate')?.textContent?.trim() || '';
    const desc = item.querySelector('description')?.textContent || '';
    const contentEncoded = item.getElementsByTagNameNS('http://purl.org/rss/1.0/modules/content/', 'encoded')[0]?.textContent || '';
    const media = item.getElementsByTagNameNS('http://search.yahoo.com/mrss/', 'content')[0]?.getAttribute('url') || '';
    const commentsText = item.getElementsByTagNameNS('http://purl.org/rss/slash/', 'comments')[0]?.textContent || '0';
    const comments = parseInt(commentsText.replace(/[^0-9]/g, '') || '0', 10);

    const img = firstImage(contentEncoded) || media || firstImage(desc);
    out.push({
      title: decodeEntities(stripTags(title)).trim(),
      link,
      pubDate,
      dateMillis: parseDate(pubDate),
      dateText: niceDate(pubDate),
      excerpt: makeExcerpt(desc),
      imageUrl: img,
      comments
    });
  });
  return out;
}

function parseSearch(jsonText) {
  let arr;
  try {
    arr = JSON.parse(jsonText);
  } catch (e) {
    return [];
  }
  if (!Array.isArray(arr)) return [];
  return arr.map(post => {
    const link = post.link || '';
    const titleObj = post.title || {};
    const excerptObj = post.excerpt || {};
    const date = post.date || '';
    return {
      title: decodeEntities(stripTags(titleObj.rendered || '')).trim(),
      link,
      dateMillis: parseDate(date),
      dateText: niceDate(date),
      excerpt: makeExcerpt(excerptObj.rendered || ''),
      imageUrl: findWpImage(post),
      comments: 0
    };
  }).filter(a => a.title && a.link);
}

function shuffleInsert(list, article, rng) {
  if (state.sortMode === SORT_RND && state.shuffled && state.shuffledBase === list) {
    state.shuffled.splice(Math.floor(rng.next() * (state.shuffled.length + 1)), 0, article);
  }
}

function computeVisible() {
  const base = state.searchQuery ? state.searchResults : state.articles;
  if (state.sortMode === SORT_RND) {
    if (!state.shuffled || state.shuffledBase !== base) {
      state.shuffled = [...base].sort(() => Math.random() - 0.5);
      state.shuffledBase = base;
    }
    return [...state.shuffled];
  }
  const copy = [...base];
  copy.sort((a, b) => {
    if (state.sortMode === SORT_TOP && a.comments !== b.comments) {
      return b.comments - a.comments;
    }
    return b.dateMillis - a.dateMillis;
  });
  return copy;
}

function buildPage(article) {
  const page = document.createElement('div');
  page.className = 'page';

  const img = document.createElement('img');
  img.className = 'bg';
  img.alt = '';
  img.dataset.src = article.imageUrl || '';
  img.onload = () => img.classList.add('loaded');
  img.onerror = () => img.classList.remove('loaded');
  page.appendChild(img);

  const grad = document.createElement('div');
  grad.className = 'gradient';
  page.appendChild(grad);

  const content = document.createElement('div');
  content.className = 'content';

  const kicker = document.createElement('div');
  kicker.className = 'kicker';
  kicker.textContent = 'HACKADAY  ·  HACKSCROLL';
  content.appendChild(kicker);

  const title = document.createElement('div');
  title.className = 'title';
  title.textContent = article.title;
  content.appendChild(title);

  let metaText = article.dateText || '';
  if (article.comments > 0) {
    metaText += (metaText ? '   ·   ' : '') + article.comments + (article.comments === 1 ? ' comment' : ' comments');
  }
  if (metaText) {
    const meta = document.createElement('div');
    meta.className = 'meta';
    meta.textContent = metaText;
    content.appendChild(meta);
  }

  if (article.excerpt) {
    const ex = document.createElement('div');
    ex.className = 'excerpt';
    ex.textContent = article.excerpt;
    content.appendChild(ex);
  }

  const btn = document.createElement('button');
  btn.className = 'read-btn';
  btn.textContent = 'READ ON HACKADAY  →';
  btn.addEventListener('click', () => openLink(article.link));
  content.appendChild(btn);

  page.appendChild(content);
  state.pageRefs.push({ article, image: img });
  return page;
}

function updateVisibleImages(i) {
  state.pageRefs.forEach((ref, idx) => {
    const near = Math.abs(idx - i) <= 1;
    if (near) {
      if (ref.image.dataset.src) {
        ref.image.src = ref.image.dataset.src;
      }
    } else {
      ref.image.removeAttribute('src');
      ref.image.classList.remove('loaded');
    }
  });
}

function updateMatchInfo() {
  if (!state.searchQuery) {
    els.matchInfo.classList.remove('visible');
    return;
  }
  els.matchInfo.classList.add('visible');
  let text = '';
  if (state.searchResults.length === 1) text = '1 match';
  else text = state.searchResults.length + ' matches';
  text += ' · ' + state.searchResults.length + ' loaded';
  if (state.noMoreSearch) text += ' · end of results';
  else text += ' · swipe to the last page for more';
  els.matchInfo.textContent = text;
}

function setLoadingPill(show, text) {
  if (text) els.loading.textContent = text;
  els.loading.style.display = show ? 'block' : 'none';
}

function flashOfflinePill() {
  els.offline.style.opacity = '1';
  els.offline.style.visibility = 'visible';
  setTimeout(() => {
    els.offline.style.opacity = '0';
  }, 200);
  setTimeout(() => {
    els.offline.style.visibility = 'hidden';
  }, 2600);
}

function showError(title, button, callback) {
  els.error.querySelector('.error-title').textContent = title;
  els.errorBtn.textContent = button;
  els.errorBtn.onclick = () => {
    els.error.classList.add('hidden');
    callback();
  };
  els.error.classList.remove('hidden');
}

function hideError() {
  els.error.classList.add('hidden');
}

function rebuild(preserve) {
  let savedLink = null;
  if (preserve && state.visible.length > 0) {
    const idx = Math.min(state.currentPage, state.visible.length - 1);
    savedLink = state.visible[idx]?.link;
  }

  state.visible = computeVisible();
  els.pager.innerHTML = '';
  state.pageRefs.length = 0;

  state.visible.forEach(article => {
    const page = buildPage(article);
    els.pager.appendChild(page);
  });

  let target = 0;
  if (state.pendingStartPage >= 0) {
    target = Math.min(state.pendingStartPage, Math.max(0, state.visible.length - 1));
    state.pendingStartPage = -1;
  } else if (savedLink) {
    const found = state.visible.findIndex(a => a.link === savedLink);
    if (found >= 0) target = found;
  }

  state.currentPage = target;
  els.pager.scrollTop = target * els.pager.clientHeight;
  updateVisibleImages(target);
  updateMatchInfo();
  state.renderedOnce = true;
  hideError();
}

function insertArticles(sourceList, sourceLinks, newArticles) {
  let added = 0;
  newArticles.forEach(article => {
    if (article.link && !sourceLinks.has(article.link)) {
      sourceLinks.add(article.link);
      sourceList.push(article);
      shuffleInsert(sourceList, article, Math);
      added++;
    }
  });
  return added;
}

async function loadPage(page) {
  if (state.loading) return;
  state.loading = true;
  setLoadingPill(true, page === 1 ? 'LOADING…' : 'LOADING MORE…');

  try {
    const res = await fetch('/api/feed?page=' + page);
    const cached = res.headers.get('X-HackScroll-Cached') === '1';
    const xml = await res.text();
    const parsed = parseRss(xml);

    if (page === 1) {
      state.articles.length = 0;
      state.loadedLinks.clear();
      state.shuffled = null;
      state.shuffledBase = null;
    }

    const added = insertArticles(state.articles, state.loadedLinks, parsed);

    if (parsed.length === 0 && page > 1) {
      state.noMoreBrowse = true;
    }
    state.nextBrowsePage = page + 1;
    if (page >= PAGE_LIMIT) state.noMoreBrowse = true;

    state.loading = false;
    setLoadingPill(false);
    if (cached) flashOfflinePill();

    if (parsed.length === 0 && !state.renderedOnce) {
      showError('Can’t reach Hackaday', 'RETRY', () => loadPage(page));
      return;
    }

    rebuild(page !== 1);
  } catch (err) {
    state.loading = false;
    setLoadingPill(false);
    if (!state.renderedOnce || state.visible.length === 0) {
      showError('Can’t reach Hackaday', 'RETRY', () => loadPage(page));
    } else {
      flashOfflinePill();
    }
  }
}

async function runSearch(page) {
  if (state.loading) return;
  state.loading = true;
  setLoadingPill(true, state.renderedOnce ? 'SEARCHING…' : 'LOADING…');

  try {
    const res = await fetch('/api/search?q=' + encodeURIComponent(state.searchQuery) + '&page=' + page);
    const json = await res.text();
    const parsed = parseSearch(json);

    if (page === 1) {
      state.searchResults.length = 0;
      state.searchLinks.clear();
      state.shuffled = null;
      state.shuffledBase = null;
    }

    insertArticles(state.searchResults, state.searchLinks, parsed);
    state.nextSearchPage = page + 1;
    if (parsed.length === 0 || state.searchResults.length >= 1000) {
      state.noMoreSearch = true;
    }

    state.loading = false;
    setLoadingPill(false);

    if (state.searchResults.length === 0 && page === 1) {
      showError('No results for “' + state.searchQuery + '”', 'CLEAR', clearSearch);
    }
    rebuild(page !== 1);
  } catch (err) {
    state.loading = false;
    setLoadingPill(false);
    if (!state.renderedOnce || state.visible.length === 0) {
      showError('Can’t reach Hackaday', 'RETRY', () => runSearch(page));
    } else {
      flashOfflinePill();
    }
  }
}

function maybeLoadMore(i) {
  if (!state.renderedOnce || state.loading || state.visible.length === 0) return;
  if (i < state.visible.length - 1) return;
  if (state.searchQuery) {
    if (!state.noMoreSearch) runSearch(state.nextSearchPage);
  } else {
    if (!state.noMoreBrowse) loadPage(state.nextBrowsePage);
  }
}

function styleSortPills() {
  els.sortDate.classList.toggle('active', state.sortMode === SORT_DATE);
  els.sortTop.classList.toggle('active', state.sortMode === SORT_TOP);
  els.sortRnd.classList.toggle('active', state.sortMode === SORT_RND);

  const searching = !!state.searchQuery;
  els.sortTop.classList.toggle('disabled', searching);
  if (searching && state.sortMode === SORT_TOP) {
    setSort(SORT_DATE);
  }
}

function setSort(mode) {
  if (mode === SORT_TOP && state.searchQuery) return;
  if (state.sortMode === mode && mode !== SORT_RND) return;
  state.sortMode = mode;
  state.shuffled = null;
  state.shuffledBase = null;
  if (mode === SORT_RND) {
    const base = state.searchQuery ? state.searchResults : state.articles;
    if (base.length > 0) {
      state.pendingStartPage = Math.floor(Math.random() * base.length);
    }
  }
  styleSortPills();
  rebuild(false);
}

function expandSearch() {
  els.searchToggle.classList.add('hidden');
  els.searchExpanded.classList.remove('hidden');
  els.searchInput.focus();
}

function hideSearchBox() {
  els.searchExpanded.classList.add('hidden');
  els.searchToggle.classList.remove('hidden');
  els.searchInput.blur();
}

function submitSearch() {
  const q = els.searchInput.value.trim();
  if (!q || q === state.searchQuery) return;
  state.searchQuery = q;
  state.searchResults.length = 0;
  state.searchLinks.clear();
  state.nextSearchPage = 1;
  state.noMoreSearch = false;
  hideSearchBox();
  styleSortPills();
  updateMatchInfo();
  runSearch(1);
}

function clearSearch() {
  state.searchQuery = '';
  state.searchResults.length = 0;
  state.searchLinks.clear();
  els.searchInput.value = '';
  hideSearchBox();
  styleSortPills();
  updateMatchInfo();
  rebuild(false);
}

function onRefreshTap() {
  if (state.loading) return;
  els.refresh.style.transition = 'transform 0.5s';
  els.refresh.style.transform = 'rotate(360deg)';
  setTimeout(() => { els.refresh.style.transition = 'none'; els.refresh.style.transform = 'rotate(0deg)'; }, 500);
  if (state.searchQuery) runSearch(1);
  else loadPage(1);
}

function openLink(url) {
  if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.openLink) {
    window.webkit.messageHandlers.openLink.postMessage(url);
    return;
  }
  fetch('/api/open?url=' + encodeURIComponent(url)).catch(() => {});
}

function hideHint() {
  if (!els.hint.classList.contains('hidden')) {
    localStorage.setItem('hackscroll-hint', '1');
    els.hint.classList.add('hidden');
  }
}

function onScroll() {
  const h = els.pager.clientHeight || window.innerHeight;
  const i = Math.max(0, Math.min(state.pageRefs.length - 1, Math.round(els.pager.scrollTop / h)));
  if (i !== state.currentPage) {
    state.currentPage = i;
    if (state.firstPageEvent) {
      state.firstPageEvent = false;
      return;
    }
    hideHint();
    updateVisibleImages(i);
    maybeLoadMore(i);
  }
}

let scrollTimer;
els.pager.addEventListener('scroll', () => {
  window.cancelAnimationFrame(scrollTimer);
  scrollTimer = window.requestAnimationFrame(onScroll);
}, { passive: true });

els.sortDate.addEventListener('click', () => setSort(SORT_DATE));
els.sortTop.addEventListener('click', () => setSort(SORT_TOP));
els.sortRnd.addEventListener('click', () => setSort(SORT_RND));
els.refresh.addEventListener('click', onRefreshTap);
els.searchToggle.addEventListener('click', expandSearch);
els.searchGo.addEventListener('click', submitSearch);
els.searchClear.addEventListener('click', clearSearch);
els.searchInput.addEventListener('keydown', e => {
  if (e.key === 'Enter') submitSearch();
});

window.addEventListener('keydown', e => {
  if (e.key === 'ArrowDown' || e.key === 'PageDown') {
    els.pager.scrollBy({ top: els.pager.clientHeight, behavior: 'smooth' });
  } else if (e.key === 'ArrowUp' || e.key === 'PageUp') {
    els.pager.scrollBy({ top: -els.pager.clientHeight, behavior: 'smooth' });
  }
});

if (els.scrollUp && els.scrollDown) {
  els.scrollUp.addEventListener('click', () => {
    els.pager.scrollBy({ top: -els.pager.clientHeight, behavior: 'smooth' });
  });
  els.scrollDown.addEventListener('click', () => {
    els.pager.scrollBy({ top: els.pager.clientHeight, behavior: 'smooth' });
  });
}

window.addEventListener('resize', () => {
  if (state.pageRefs.length > 0) {
    els.pager.scrollTop = state.currentPage * els.pager.clientHeight;
  }
});

styleSortPills();
if (!state.hintShown) {
  els.hint.classList.remove('hidden');
}
loadPage(1);
