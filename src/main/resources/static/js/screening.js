// === VCogniRule Screening — tabs + one question at a time ===
//
// The page is a single <form>. Every question stays in the DOM (hidden ones are
// still submitted), so this script only controls what is visible:
//   - one section panel at a time (tabs)
//   - one question card at a time inside a panel
// The server decides which sections count as complete; this script mirrors that
// rule (all questions answered) to show progress and to warn before submitting.

(function () {
    'use strict';

    const form = document.getElementById('screening-form');
    if (!form) return;

    const page = document.querySelector('.assessment-page');
    const tabs = Array.from(form.querySelectorAll('[data-tab]'));
    const panels = Array.from(form.querySelectorAll('[data-panel]'));
    const summary = form.querySelector('[data-summary]');
    const dialog = document.getElementById('submit-dialog');

    const SAFETY = 'C-SSRS';
    const DEPRESSION = 'PHQ-9';
    const AUTO_ADVANCE_MS = 280;

    const position = {};          // section code -> index of the visible card (cards.length = "done" screen)
    let answeredByPointer = false; // auto-advance only for mouse/touch, not keyboard arrow keys
    let confirmed = false;         // set when the user confirms in the dialog

    // ---------- helpers ----------

    const panelFor = code => panels.find(p => p.dataset.panel === code);
    const tabFor = code => tabs.find(t => t.dataset.tab === code);
    const cardsOf = panel => Array.from(panel.querySelectorAll('.q-card'));
    const labelOf = code => panelFor(code).dataset.label;

    function isAnswered(card) {
        const radios = card.querySelectorAll('input[type="radio"]');
        if (radios.length) return Array.from(radios).some(r => r.checked);
        const input = card.querySelector('input');
        return !!input && input.value.trim() !== '';
    }

    function stats(panel) {
        const cards = cardsOf(panel);
        const answered = cards.filter(isAnswered).length;
        return { answered, total: cards.length, complete: answered === cards.length };
    }

    function firstUnanswered(panel) {
        const idx = cardsOf(panel).findIndex(c => !isAnswered(c));
        return idx === -1 ? cardsOf(panel).length : idx;
    }

    // ---------- rendering ----------

    function showCard(panel, index, moveFocus) {
        const cards = cardsOf(panel);
        const code = panel.dataset.panel;
        const done = index >= cards.length;
        position[code] = Math.min(index, cards.length);

        cards.forEach((c, i) => c.classList.toggle('active', i === index));
        panel.querySelector('[data-done]').hidden = !done;
        panel.querySelector('.q-nav').hidden = done;
        panel.querySelector('[data-hint]').hidden = true;

        if (!done) {
            panel.querySelector('[data-prev]').disabled = index === 0;
            panel.querySelector('[data-next]').textContent =
                index === cards.length - 1 ? 'Finish section' : 'Next →';
        }

        if (moveFocus) {
            const target = done
                ? panel.querySelector('[data-next-section]')
                : (cards[index].querySelector('input:checked') || cards[index].querySelector('input'));
            if (target) target.focus({ preventScroll: true });
        }
        updateStatus();
    }

    function openSection(code, index, moveFocus) {
        const panel = panelFor(code);
        if (!panel) return;
        tabs.forEach(t => {
            const active = t.dataset.tab === code;
            t.setAttribute('aria-selected', active ? 'true' : 'false');
            t.tabIndex = active ? 0 : -1;
        });
        panels.forEach(p => p.classList.toggle('active', p === panel));

        let start = index;
        if (start === undefined) {
            start = position[code] !== undefined ? position[code] : firstUnanswered(panel);
            // A completed section opens on its "done" screen, an unfinished one at its first gap
            if (start >= cardsOf(panel).length && !stats(panel).complete) start = firstUnanswered(panel);
        }
        showCard(panel, start, moveFocus);

        const tab = tabFor(code);
        if (tab && tab.scrollIntoView) tab.scrollIntoView({ block: 'nearest', inline: 'center' });
        const top = panel.getBoundingClientRect().top;
        if (moveFocus && (top < 0 || top > window.innerHeight * 0.6)) {
            panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    function updateStatus() {
        let completeCount = 0;
        panels.forEach(panel => {
            const code = panel.dataset.panel;
            const s = stats(panel);
            const tab = tabFor(code);
            const badge = tab.querySelector('[data-status]');
            const mandatory = tab.dataset.mandatory === 'true';

            tab.classList.remove('is-complete', 'is-partial', 'is-required');
            if (s.complete) {
                completeCount++;
                tab.classList.add('is-complete');
                badge.textContent = '✓';
                badge.setAttribute('aria-label', 'complete');
            } else if (s.answered > 0) {
                tab.classList.add('is-partial');
                badge.textContent = s.answered + '/' + s.total;
                badge.setAttribute('aria-label', s.answered + ' of ' + s.total + ' answered');
            } else if (mandatory) {
                tab.classList.add('is-required');
                badge.textContent = 'Required';
                badge.removeAttribute('aria-label');
            } else {
                badge.textContent = '';
            }

            // Progress bar inside the panel
            panel.querySelector('[data-progress-text]').textContent =
                s.answered + ' of ' + s.total + ' answered';
            panel.querySelector('[data-progress-fill]').style.width =
                (s.total ? (s.answered / s.total) * 100 : 0) + '%';
        });

        const safetyDone = stats(panelFor(SAFETY)).complete;
        summary.innerHTML = '';
        const strong = document.createElement('strong');
        strong.textContent = completeCount + ' of ' + panels.length;
        summary.append(strong, ' sections complete');
        if (!safetyDone) summary.append(' · Complete the Safety Check to submit');
    }

    // ---------- navigation ----------

    function advance(panel) {
        const cards = cardsOf(panel);
        const code = panel.dataset.panel;
        const next = position[code] + 1;
        if (next < cards.length) {
            showCard(panel, next, true);
        } else {
            finishSection(panel);
        }
    }

    function finishSection(panel) {
        if (stats(panel).complete) {
            showCard(panel, cardsOf(panel).length, true);
        } else {
            const gap = firstUnanswered(panel);
            const missing = stats(panel).total - stats(panel).answered;
            showCard(panel, gap, true);
            const hint = panel.querySelector('[data-hint]');
            hint.textContent = missing === 1
                ? '1 question in this section is still unanswered. Sections are only included in your report when every question is answered.'
                : missing + ' questions in this section are still unanswered. Sections are only included in your report when every question is answered.';
            hint.hidden = false;
        }
    }

    function nextIncompleteSection(fromCode) {
        const order = panels.map(p => p.dataset.panel);
        const start = order.indexOf(fromCode);
        for (let i = 1; i <= order.length; i++) {
            const code = order[(start + i) % order.length];
            if (!stats(panelFor(code)).complete) return code;
        }
        return null;
    }

    // ---------- events ----------

    tabs.forEach((tab, i) => {
        tab.addEventListener('click', () => openSection(tab.dataset.tab, undefined, true));
        // Arrow keys move between tabs (WAI-ARIA tabs pattern)
        tab.addEventListener('keydown', e => {
            if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
            e.preventDefault();
            const next = tabs[(i + (e.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length];
            next.focus();
            openSection(next.dataset.tab);
        });
    });

    panels.forEach(panel => {
        panel.querySelector('[data-prev]').addEventListener('click', () => {
            const code = panel.dataset.panel;
            if (position[code] > 0) showCard(panel, position[code] - 1, true);
        });
        panel.querySelector('[data-next]').addEventListener('click', () => advance(panel));
        panel.querySelector('[data-review]').addEventListener('click', () => showCard(panel, 0, true));
        panel.querySelector('[data-next-section]').addEventListener('click', () => {
            const next = nextIncompleteSection(panel.dataset.panel);
            if (next) {
                openSection(next, undefined, true);
            } else {
                form.querySelector('[data-submit]').focus();
                form.querySelector('.submit-bar').scrollIntoView({ behavior: 'smooth', block: 'end' });
            }
        });

        panel.addEventListener('pointerdown', e => {
            if (e.target.closest('.option')) answeredByPointer = true;
        });
        panel.addEventListener('keydown', () => { answeredByPointer = false; });

        panel.addEventListener('change', e => {
            updateStatus();
            if (e.target.type === 'radio' && answeredByPointer) {
                answeredByPointer = false;
                const card = e.target.closest('.q-card');
                setTimeout(() => {
                    // Only advance if the user is still on the same card
                    if (card.classList.contains('active')) advance(panel);
                }, AUTO_ADVANCE_MS);
            }
        });
        panel.addEventListener('input', e => {
            if (e.target.type === 'number' || e.target.type === 'time') updateStatus();
        });
        // Enter in a number/time field moves on instead of submitting the whole form
        panel.addEventListener('keydown', e => {
            if (e.key === 'Enter' && (e.target.type === 'number' || e.target.type === 'time')) {
                e.preventDefault();
                advance(panel);
            }
        });
    });

    // ---------- submit + confirmation ----------

    function fillList(list, items) {
        list.innerHTML = '';
        items.forEach(text => {
            const li = document.createElement('li');
            li.textContent = text;
            list.appendChild(li);
        });
    }

    form.addEventListener('submit', e => {
        if (confirmed) return; // confirmed in the dialog: let the browser submit

        e.preventDefault();

        if (!stats(panelFor(SAFETY)).complete) {
            openSection(SAFETY, firstUnanswered(panelFor(SAFETY)), true);
            const hint = panelFor(SAFETY).querySelector('[data-hint]');
            hint.textContent = 'The Safety Check is required. Please answer every question in this section before submitting.';
            hint.hidden = false;
            return;
        }

        const included = [];
        const partial = [];
        panels.forEach(p => {
            const s = stats(p);
            const name = labelOf(p.dataset.panel) + ' (' + p.dataset.panel + ')';
            if (s.complete) included.push(name);
            else if (s.answered > 0) partial.push(name + ': ' + s.answered + ' of ' + s.total + ' answered');
        });

        const phq9Missing = !stats(panelFor(DEPRESSION)).complete;
        dialog.querySelector('[data-dlg-phq9]').hidden = !phq9Missing;
        dialog.querySelector('[data-dlg-phq9-go]').hidden = !phq9Missing;
        dialog.querySelector('[data-dlg-partial]').hidden = partial.length === 0;
        fillList(dialog.querySelector('[data-dlg-partial-list]'), partial);
        fillList(dialog.querySelector('[data-dlg-included-list]'), included);

        const submitBtn = dialog.querySelector('[data-dlg-submit]');
        submitBtn.textContent = phq9Missing ? 'Submit without it' : 'Submit';
        submitBtn.className = phq9Missing ? 'btn btn-outline' : 'btn btn-primary';

        if (typeof dialog.showModal === 'function') {
            dialog.showModal();
        } else {
            // Very old browsers: fall back to a plain confirm
            if (window.confirm('Submit your screening now?')) { confirmed = true; form.submit(); }
        }
    });

    dialog.querySelector('[data-dlg-submit]').addEventListener('click', () => { confirmed = true; });
    dialog.querySelector('[data-dlg-back]').addEventListener('click', () => dialog.close());
    dialog.querySelector('[data-dlg-phq9-go]').addEventListener('click', () => {
        dialog.close();
        openSection(DEPRESSION, firstUnanswered(panelFor(DEPRESSION)), true);
    });

    // ---------- start ----------

    const resume = page.dataset.resume;
    let startCode = resume && panelFor(resume) ? resume : null;
    if (!startCode) {
        // First unfinished section in tab order (the Safety Check comes first)
        const firstOpen = panels.find(p => !stats(p).complete);
        startCode = (firstOpen || panels[0]).dataset.panel;
    }
    const startPanel = panelFor(startCode);
    openSection(startCode, firstUnanswered(startPanel) < cardsOf(startPanel).length
        ? firstUnanswered(startPanel) : 0, false);
    updateStatus();
})();
