(() => {
  const root = document.querySelector('[data-defense-calendar]');
  if (!root) return;

  const eventsUrl = root.dataset.eventsUrl;
  const grid = root.querySelector('[data-calendar-grid]');
  const list = root.querySelector('[data-calendar-list]');
  const title = root.querySelector('[data-calendar-title]');
  const loading = root.querySelector('[data-calendar-loading]');
  const empty = root.querySelector('[data-calendar-empty]');
  const error = root.querySelector('[data-calendar-error]');
  const dialog = root.querySelector('[data-calendar-dialog]');
  const monthViewButton = root.querySelector('[data-calendar-month-view]');
  const listViewButton = root.querySelector('[data-calendar-list-view]');
  let cursor = new Date();
  cursor = new Date(cursor.getFullYear(), cursor.getMonth(), 1);
  let events = [];

  const two = value => String(value).padStart(2, '0');
  const localIso = date => `${date.getFullYear()}-${two(date.getMonth() + 1)}-${two(date.getDate())}T${two(date.getHours())}:${two(date.getMinutes())}:${two(date.getSeconds())}`;
  const localDateKey = value => value.slice(0, 10);
  const formatTime = value => new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date(value));
  const formatDateTime = value => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));

  const setView = view => {
    root.dataset.view = view;
    monthViewButton.setAttribute('aria-pressed', String(view === 'month'));
    listViewButton.setAttribute('aria-pressed', String(view === 'list'));
  };

  const showDetails = event => {
    dialog.querySelector('[data-detail-title]').textContent = event.title;
    dialog.querySelector('[data-detail-time]').textContent = `${formatDateTime(event.start)} – ${formatDateTime(event.end)}`;
    dialog.querySelector('[data-detail-room]').textContent = event.room;
    dialog.querySelector('[data-detail-council]').textContent = event.councilName;
    dialog.querySelector('[data-detail-supervisor]').textContent = event.supervisorName;
    dialog.querySelector('[data-detail-student]').textContent = event.studentName;
    const link = dialog.querySelector('[data-detail-link]');
    link.href = event.detailUrl;
    dialog.showModal();
  };

  const eventButton = event => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'calendar-event';
    button.setAttribute('aria-label', `${formatTime(event.start)} ${event.title}, phòng ${event.room}`);
    const time = document.createElement('strong'); time.textContent = formatTime(event.start);
    const text = document.createElement('span'); text.textContent = event.title;
    const room = document.createElement('small'); room.textContent = event.room;
    button.append(time, text, room);
    button.addEventListener('click', () => showDetails(event));
    return button;
  };

  const render = () => {
    grid.replaceChildren(); list.replaceChildren();
    title.textContent = new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' }).format(cursor);
    const firstWeekday = (cursor.getDay() + 6) % 7;
    const firstCell = new Date(cursor.getFullYear(), cursor.getMonth(), 1 - firstWeekday);
    for (let index = 0; index < 42; index += 1) {
      const day = new Date(firstCell); day.setDate(firstCell.getDate() + index);
      const cell = document.createElement('section'); cell.className = 'calendar-day';
      if (day.getMonth() !== cursor.getMonth()) cell.classList.add('is-outside');
      const heading = document.createElement('h3'); heading.textContent = String(day.getDate());
      const now = new Date();
      if (day.toDateString() === now.toDateString()) {
        cell.classList.add('is-today');
        heading.setAttribute('aria-current', 'date');
        heading.setAttribute('aria-label', `${day.getDate()}, hôm nay`);
      }
      cell.append(heading);
      const key = `${day.getFullYear()}-${two(day.getMonth() + 1)}-${two(day.getDate())}`;
      events.filter(event => localDateKey(event.start) === key).forEach(event => cell.append(eventButton(event)));
      grid.append(cell);
    }
    events.forEach(event => {
      const row = document.createElement('article'); row.className = 'calendar-list-item';
      const date = document.createElement('time'); date.dateTime = event.start; date.textContent = formatDateTime(event.start);
      row.append(date, eventButton(event)); list.append(row);
    });
    empty.hidden = events.length !== 0;
  };

  const load = async () => {
    loading.hidden = false; empty.hidden = true; error.hidden = true;
    const start = new Date(cursor.getFullYear(), cursor.getMonth(), 1);
    const end = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 1);
    try {
      const response = await fetch(`${eventsUrl}?start=${encodeURIComponent(localIso(start))}&end=${encodeURIComponent(localIso(end))}`, { headers: { Accept: 'application/json' } });
      if (!response.ok) throw new Error('calendar request failed');
      events = await response.json(); render();
    } catch (_) {
      grid.replaceChildren(); list.replaceChildren(); error.hidden = false;
    } finally {
      loading.hidden = true;
    }
  };

  root.querySelector('[data-calendar-prev]').addEventListener('click', () => { cursor.setMonth(cursor.getMonth() - 1); load(); });
  root.querySelector('[data-calendar-next]').addEventListener('click', () => { cursor.setMonth(cursor.getMonth() + 1); load(); });
  root.querySelector('[data-calendar-today]').addEventListener('click', () => { const now = new Date(); cursor = new Date(now.getFullYear(), now.getMonth(), 1); load(); });
  monthViewButton.addEventListener('click', () => setView('month'));
  listViewButton.addEventListener('click', () => setView('list'));
  dialog.querySelector('[data-dialog-close]').addEventListener('click', () => dialog.close());
  setView(window.matchMedia('(max-width: 767.98px)').matches ? 'list' : 'month');
  load();
})();
