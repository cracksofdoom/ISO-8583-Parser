const $ = id => document.getElementById(id);

const EXAMPLE_A = '0800822000000000000004000000000000001125210025123456301';
const EXAMPLE_B = '081082200000020000000400000000000000112521002512345600301';
const DEFAULT_NOTICE = 'Paste an ISO 8583 frame above, then choose Inspect and decode.';
const DECODE_LABEL = 'Inspect and decode';

const state = { result: null, comparison: false, side: 'left', tab: 'fields', busy: false };

/* ---------- Small helpers ---------- */

const store = {
  get(key) { try { return localStorage.getItem(key); } catch { return null; } },
  set(key, value) { try { localStorage.setItem(key, value); } catch { /* storage unavailable */ } }
};

function node(tag, text, className) {
  const el = document.createElement(tag);
  if (text !== undefined && text !== null) el.textContent = text;
  if (className) el.className = className;
  return el;
}

function plural(n, word) { return `${n} ${word}${n === 1 ? '' : 's'}`; }

async function copyText(text, button, idleLabel) {
  try {
    await navigator.clipboard.writeText(text);
    button.textContent = 'Copied';
  } catch {
    button.textContent = 'Copy failed';
  }
  setTimeout(() => { button.textContent = idleLabel; }, 1600);
}

/* ---------- Input handling ---------- */

const isHexFormat = () => $('format').value === 'HEX_BINARY_BITMAP';

// Hex input ignores whitespace; ASCII input only loses stray line breaks at the ends.
function cleaned(raw) {
  return isHexFormat() ? raw.replace(/\s+/g, '') : raw.replace(/^[\r\n]+|[\r\n]+$/g, '');
}

function updateCount(inputId, countId, hintId) {
  const value = cleaned($(inputId).value);
  const hint = $(hintId);
  hint.textContent = '';
  hint.classList.remove('warn');
  if (isHexFormat()) {
    $(countId).textContent = `${plural(Math.floor(value.length / 2), 'byte')}, ${plural(value.length, 'hex digit')}`;
    if (/[^0-9a-f]/i.test(value)) hint.textContent = 'Contains characters that are not hex.';
    else if (value.length % 2 === 1) hint.textContent = 'Odd number of hex digits.';
    if (hint.textContent) hint.classList.add('warn');
  } else {
    $(countId).textContent = plural(value.length, 'character');
  }
}

function updateCounts() {
  updateCount('message', 'countA', 'hintA');
  updateCount('compareMessage', 'countB', 'hintB');
}

function updateFormatHint() {
  $('formatHint').textContent = isHexFormat()
    ? 'Hex bytes: enter the entire frame as byte pairs. Spaces, colons, and hyphens are allowed.'
    : 'ASCII frame: four MTI digits, a hexadecimal bitmap, then field data. Use the hex option for binary frames.';
}

['message', 'compareMessage'].forEach(id => {
  $(id).addEventListener('input', updateCounts);
  $(id).addEventListener('keydown', event => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') { event.preventDefault(); decode(); }
  });
});

document.querySelectorAll('[data-clear]').forEach(button => {
  button.addEventListener('click', () => {
    const target = $(button.dataset.clear);
    target.value = '';
    updateCounts();
    target.focus();
  });
});

document.querySelectorAll('[data-paste]').forEach(button => {
  button.addEventListener('click', async () => {
    const target = $(button.dataset.paste);
    const hint = $(target.id === 'message' ? 'hintA' : 'hintB');
    try {
      target.value = await navigator.clipboard.readText();
      updateCounts();
      target.focus();
    } catch {
      hint.textContent = 'Clipboard access was blocked. Click the box and press Ctrl+V or Cmd+V.';
      hint.classList.add('warn');
    }
  });
});

$('swap').addEventListener('click', () => {
  const a = $('message').value;
  $('message').value = $('compareMessage').value;
  $('compareMessage').value = a;
  updateCounts();
});

function loadExample(scrollToWorkbench = false) {
  $('format').value = 'ASCII_HEX_BITMAP';
  store.set('Parser.format', $('format').value);
  $('header').value = '0';
  store.set('Parser.header', '0');
  $('message').value = EXAMPLE_A;
  $('compareMessage').value = EXAMPLE_B;
  updateCounts();
  updateFormatHint();
  if (scrollToWorkbench) $('workbench').scrollIntoView();
  $('decode').focus({ preventScroll: true });
}

$('example').addEventListener('click', () => loadExample());
$('heroExample').addEventListener('click', () => loadExample(true));

$('clear').addEventListener('click', () => {
  $('message').value = '';
  $('compareMessage').value = '';
  resetResults();
  updateCounts();
  $('message').focus();
});

/* ---------- Settings (messages are never persisted) ---------- */

(function restoreSettings() {
  const format = store.get('Parser.format');
  if (format && [...$('format').options].some(o => o.value === format)) $('format').value = format;
  const header = store.get('Parser.header');
  if (header !== null && header !== '') $('header').value = header;
  updateCounts();
  updateFormatHint();
})();

[['format', 'Parser.format', el => el.value],
 ['header', 'Parser.header', el => el.value]].forEach(([id, key, read]) => {
  $(id).addEventListener('change', () => {
    store.set(key, read($(id)));
    updateCounts();
    updateFormatHint();
    // Re-run so the visible results always match the selected options.
    if (state.result && !state.busy) decode({ scroll: false });
  });
});

/* ---------- Notices and reset ---------- */

function setNotice(text, kind = 'info') {
  const el = $('notice');
  el.textContent = text;
  el.className = kind === 'info' ? 'notice' : `notice ${kind}`;
  el.setAttribute('role', kind === 'error' ? 'alert' : 'status');
  el.hidden = false;
}

function hideOutput() {
  $('output').hidden = true;
  $('summary').hidden = true;
  $('copy').hidden = true;
  $('download').hidden = true;
}

function resetResults() {
  state.result = null;
  hideOutput();
  setNotice(DEFAULT_NOTICE);
}

/* ---------- Rendering ---------- */

const DIFF_LABELS = { changed: 'Changed', left_only: 'Only in A', right_only: 'Only in B', same: 'Same', unchanged: 'Same', equal: 'Same' };
const SAME_STATUSES = ['same', 'unchanged', 'equal', 'match', 'identical'];
const isDiff = status => !SAME_STATUSES.includes(String(status).toLowerCase());
const statusLabel = status => {
  const key = String(status).toLowerCase();
  if (DIFF_LABELS[key]) return DIFF_LABELS[key];
  const text = key.replace(/_/g, ' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
};

const primaryFrame = () => (state.comparison ? state.result[state.side] : state.result);

function renderSummary() {
  const frame = primaryFrame();
  const leftover = frame.totalBytes - frame.consumedBytes;
  $('summary').replaceChildren();
  const chips = [
    ['MTI', frame.mti],
    ['Fields present', frame.presentFields.length],
    ['Bitmap', frame.bitmap],
    ['Bytes decoded', `${frame.consumedBytes} of ${frame.totalBytes}`]
  ];
  chips.forEach(([label, value], index) => {
    const chip = node('div', null, leftover !== 0 && index === 3 ? 'chip warn' : 'chip');
    chip.append(node('span', label), node('strong', value));
    $('summary').append(chip);
  });
  $('summary').hidden = false;
}

function renderFields() {
  const frame = primaryFrame();
  const query = $('fieldSearch').value.trim().toLowerCase();
  const all = Object.values(frame.fields);
  const matches = all.filter(field => {
    if (!query) return true;
    const id = String(field.id).padStart(3, '0');
    return `${id} ${field.id} ${field.name} ${field.value}`.toLowerCase().includes(query);
  });

  $('fieldsCount').textContent = query ? `${matches.length}/${all.length}` : all.length;
  const list = $('fields');
  list.replaceChildren();

  if (!matches.length) {
    list.append(node('div', all.length ? 'No fields match this filter.' : 'No fields were decoded.', 'empty-state'));
    return;
  }

  matches.forEach(field => {
    const row = node('div', null, 'field');
    row.append(node('span', String(field.id).padStart(3, '0'), 'field-id'));

    const content = node('div');
    const hasValue = field.value !== '' && field.value !== null && field.value !== undefined;
    content.append(
      node('div', field.name, 'field-name'),
      node('div', hasValue ? field.value : 'Empty', hasValue ? 'field-value' : 'field-value empty')
    );
    row.append(content);

    const copy = node('button', 'Copy', 'btn btn-ghost btn-sm');
    copy.type = 'button';
    copy.setAttribute('aria-label', `Copy value of field ${field.id}, ${field.name}`);
    copy.addEventListener('click', () => copyText(String(field.value ?? ''), copy, 'Copy'));
    row.append(copy);

    list.append(row);
  });
}

// Wrap the differing middle of two strings in <mark>, keeping common prefix and suffix plain.
function markDifference(a, b) {
  let start = 0;
  const min = Math.min(a.length, b.length);
  while (start < min && a[start] === b[start]) start++;
  let end = 0;
  while (end < min - start && a[a.length - 1 - end] === b[b.length - 1 - end]) end++;
  const build = text => {
    const fragment = document.createDocumentFragment();
    fragment.append(text.slice(0, start));
    if (text.length - end > start) fragment.append(node('mark', text.slice(start, text.length - end)));
    fragment.append(text.slice(text.length - end));
    return fragment;
  };
  return [build(a), build(b)];
}

function diffRow(label, id, left, right, status) {
  const key = String(status).toLowerCase();
  const row = node('tr', null, key);

  const first = node('td');
  if (id) first.append(node('span', id, 'fid'));
  first.append(label);
  row.append(first);

  const a = node('td', null, 'a');
  const b = node('td', null, 'b');
  const hasLeft = left !== null && left !== undefined;
  const hasRight = right !== null && right !== undefined;
  if (hasLeft && hasRight && String(left) !== String(right)) {
    const [fa, fb] = markDifference(String(left), String(right));
    a.append(fa); b.append(fb);
  } else {
    a.textContent = hasLeft ? String(left) : 'Not present';
    b.textContent = hasRight ? String(right) : 'Not present';
  }
  if (!hasLeft) a.classList.add('missing');
  if (!hasRight) b.classList.add('missing');
  row.append(a, b);

  const statusCell = node('td');
  statusCell.append(node('span', statusLabel(status), `badge ${key}`));
  row.append(statusCell);
  return row;
}

function renderDiff() {
  const result = state.result;
  const body = $('diff');
  body.replaceChildren();

  const rows = [{ label: 'MTI', id: '', left: result.left.mti, right: result.right.mti, status: result.mtiStatus }];
  result.differences.forEach(item => rows.push({
    label: item.name, id: String(item.id).padStart(3, '0'), left: item.left, right: item.right, status: item.status
  }));

  const counts = {};
  rows.forEach(row => { if (isDiff(row.status)) counts[row.status] = (counts[row.status] || 0) + 1; });
  const total = Object.values(counts).reduce((sum, n) => sum + n, 0);
  $('diffCount').textContent = total;

  $('diffLegend').replaceChildren();
  if (!total) {
    $('diffLegend').append(node('span', 'No differences'));
  } else {
    Object.entries(counts).forEach(([status, n]) => {
      const pill = node('span');
      pill.append(node('strong', n), statusLabel(status));
      $('diffLegend').append(pill);
    });
  }

  const visible = $('diffOnly').checked ? rows.filter(row => isDiff(row.status)) : rows;
  if (!visible.length) {
    const cell = node('td', 'The two frames are identical.', 'missing');
    cell.colSpan = 4;
    const emptyRow = node('tr');
    emptyRow.append(cell);
    body.append(emptyRow);
    return;
  }
  visible.forEach(row => body.append(diffRow(row.label, row.id, row.left, row.right, row.status)));
}

function checkLeftover() {
  const frames = state.comparison ? [['Message A', state.result.left], ['Message B', state.result.right]] : [['The message', state.result]];
  const problems = frames
    .filter(([, frame]) => frame.consumedBytes !== frame.totalBytes)
    .map(([name, frame]) => `${name} has ${plural(frame.totalBytes - frame.consumedBytes, 'byte')} left after the last decoded field`);
  if (problems.length) {
    setNotice(`${problems.join('. ')}. Check the input format and header bytes, or the field layout for your network.`, 'warn');
  } else {
    $('notice').hidden = true;
  }
}

function setTab(name, focus) {
  state.tab = name;
  document.querySelectorAll('.tab').forEach(tab => {
    const active = tab.dataset.tab === name;
    tab.setAttribute('aria-selected', String(active));
    tab.tabIndex = active ? 0 : -1;
    $(`pane-${tab.dataset.tab}`).hidden = !active;
    if (active && focus) tab.focus();
  });
}

function render(result, comparison) {
  const wasShowing = !$('output').hidden;
  state.result = result;
  state.comparison = comparison;
  state.side = 'left';

  $('fieldSearch').value = '';
  $('sideToggle').hidden = !comparison;
  document.querySelectorAll('[data-side]').forEach(b => b.setAttribute('aria-pressed', String(b.dataset.side === 'left')));
  $('tab-diff').hidden = !comparison;

  renderSummary();
  renderFields();
  if (comparison) renderDiff();
  $('json').textContent = JSON.stringify(result, null, 2);

  $('output').hidden = false;
  $('copy').hidden = false;
  $('download').hidden = false;
  checkLeftover();

  // Open on the most useful view, unless the person is already on a valid one.
  if (!wasShowing) setTab(comparison ? 'diff' : 'fields');
  else setTab(state.tab === 'diff' && !comparison ? 'fields' : state.tab);
}

/* ---------- Result interactions ---------- */

document.querySelectorAll('.tab').forEach(tab => {
  tab.addEventListener('click', () => setTab(tab.dataset.tab));
  tab.addEventListener('keydown', event => {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    const tabs = [...document.querySelectorAll('.tab')].filter(t => !t.hidden);
    const index = tabs.indexOf(tab);
    let next = index;
    if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
    if (event.key === 'ArrowLeft') next = (index - 1 + tabs.length) % tabs.length;
    if (event.key === 'Home') next = 0;
    if (event.key === 'End') next = tabs.length - 1;
    event.preventDefault();
    setTab(tabs[next].dataset.tab, true);
  });
});

$('fieldSearch').addEventListener('input', () => { if (state.result) renderFields(); });
$('diffOnly').addEventListener('change', () => { if (state.result && state.comparison) renderDiff(); });

document.querySelectorAll('[data-side]').forEach(button => {
  button.addEventListener('click', () => {
    state.side = button.dataset.side;
    document.querySelectorAll('[data-side]').forEach(b => b.setAttribute('aria-pressed', String(b === button)));
    renderSummary();
    renderFields();
  });
});

$('copy').addEventListener('click', () => { if (state.result) copyText(JSON.stringify(state.result, null, 2), $('copy'), 'Copy JSON'); });

$('download').addEventListener('click', () => {
  if (!state.result) return;
  const blob = new Blob([JSON.stringify(state.result, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = state.comparison ? 'iso8583-comparison.json' : 'iso8583-decoded.json';
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
});

/* ---------- Decode ---------- */

function requestFor(message) {
  return {
    message: cleaned(message),
    format: $('format').value,
    headerBytes: Number($('header').value) || 0,
    revealSensitive: false
  };
}

function setBusy(busy) {
  state.busy = busy;
  $('decode').disabled = busy;
  $('decode').setAttribute('aria-busy', String(busy));
  $('decode').querySelector('.btn-label').textContent = busy ? 'Decoding...' : DECODE_LABEL;
}

async function decode({ scroll = true } = {}) {
  if (state.busy) return;
  const first = $('message').value;
  const second = $('compareMessage').value;

  if (!cleaned(first)) {
    hideOutput();
    state.result = null;
    setNotice('Message A is empty. Paste a frame or load the example.', 'error');
    $('message').focus();
    return;
  }

  const headerValue = Number($('header').value);
  if ($('header').value.trim() === '' || !Number.isInteger(headerValue) || headerValue < 0 || headerValue > 64) {
    hideOutput();
    state.result = null;
    setNotice('Header bytes must be a whole number from 0 to 64.', 'error');
    $('header').focus();
    return;
  }

  const comparison = cleaned(second).length > 0;
  const body = comparison ? { left: requestFor(first), right: requestFor(second) } : requestFor(first);

  setBusy(true);
  try {
    const response = await fetch(comparison ? '/api/v1/messages/compare' : '/api/v1/messages/parse', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    let result = null;
    try { result = await response.json(); } catch { /* handled below */ }
    if (!response.ok || !result) {
      const detail = result && result.error ? result.error : 'The decoder returned an unexpected response';
      const where = result && result.offset != null ? ` (at byte ${result.offset})` : '';
      throw new Error(`${detail}${where}`);
    }
    render(result, comparison);
    if (scroll) {
      $('resultsSection').scrollIntoView();
      $('results-title').focus({ preventScroll: true });
    }
  } catch (error) {
    state.result = null;
    hideOutput();
    const offline = error instanceof TypeError;
    setNotice(offline ? 'Could not reach the decoder service. Check your connection and try again.' : error.message, 'error');
  } finally {
    setBusy(false);
  }
}

$('decode').addEventListener('click', () => decode());
