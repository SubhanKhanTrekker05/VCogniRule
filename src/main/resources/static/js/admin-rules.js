// === VCogniRule Admin Dashboard - Search + Sort ===

document.addEventListener('DOMContentLoaded', function() {

    // ============================================================
    // FEATURE 1: Real-time search with highlighting
    // Option B: searches BOTH Rule ID and Name columns simultaneously
    // ============================================================

    const searchInput = document.getElementById('ruleSearch');
    const searchCount = document.getElementById('searchCount');
    const tableBody = document.querySelector('.rule-table tbody');

    if (!searchInput || !tableBody) return;

    // Cache all rows + both Rule ID cells and Name anchors (for restore on clear)
    const allRows = Array.from(tableBody.querySelectorAll('tr'));
    const idCells = Array.from(tableBody.querySelectorAll('tr td:nth-child(2)'));
    const nameAnchors = Array.from(tableBody.querySelectorAll('tr td:nth-child(3) a'));
    const originalIds = idCells.map(td => td.textContent);
    const originalNames = nameAnchors.map(a => a.textContent);

    function escapeHtml(str) {
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function escapeRegex(str) {
        return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    function performSearch(rawQuery) {
        const query = rawQuery.trim().toLowerCase();
        let matchCount = 0;

        allRows.forEach((row, index) => {
            const idCell = idCells[index];
            const nameAnchor = nameAnchors[index];
            const originalId = originalIds[index];
            const originalName = originalNames[index];

            if (query === '') {
                // No query: show all rows, restore both cells to original text
                row.style.display = '';
                idCell.textContent = originalId;
                nameAnchor.textContent = originalName;
                matchCount++;
            } else {
                const idMatches = originalId.toLowerCase().includes(query);
                const nameMatches = originalName.toLowerCase().includes(query);

                if (idMatches || nameMatches) {
                    // At least one column matched: show row + highlight in matching column(s)
                    row.style.display = '';
                    matchCount++;

                    const escapedQuery = escapeRegex(query);
                    const regex = new RegExp('(' + escapedQuery + ')', 'gi');

                    // Highlight in Rule ID cell (only if it matched)
                    if (idMatches) {
                        const escapedId = escapeHtml(originalId);
                        idCell.innerHTML = escapedId.replace(regex, '<mark>$1</mark>');
                    } else {
                        idCell.textContent = originalId;
                    }

                    // Highlight in Name cell (only if it matched)
                    if (nameMatches) {
                        const escapedName = escapeHtml(originalName);
                        nameAnchor.innerHTML = escapedName.replace(regex, '<mark>$1</mark>');
                    } else {
                        nameAnchor.textContent = originalName;
                    }
                } else {
                    // No match in either column: hide row
                    row.style.display = 'none';
                }
            }
        });

        // Update count display
        if (query === '') {
            searchCount.textContent = '';
        } else {
            searchCount.textContent = matchCount + ' of ' + allRows.length + ' rules match';
        }
    }

    // Listen to every keystroke
    searchInput.addEventListener('input', function(e) {
        performSearch(e.target.value);
    });

    // Bonus: Escape key clears the search
    searchInput.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            searchInput.value = '';
            performSearch('');
            searchInput.blur();
        }
    });

});

// ============================================================
// FEATURE 2: Sortable column headers
// ============================================================

document.addEventListener('DOMContentLoaded', function() {

    const table = document.querySelector('.rule-table');
    const tbody = table ? table.querySelector('tbody') : null;
    const sortableHeaders = table ? table.querySelectorAll('th.sortable') : [];

    if (!table || sortableHeaders.length === 0) return;

    let currentSortColumn = null;
    let currentSortDirection = 'asc';

    sortableHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const columnIndex = parseInt(this.dataset.column);
            const sortType = this.dataset.sort;  // 'numeric' or 'text'

            // Determine direction: toggle if same column, else default to asc
            if (currentSortColumn === this) {
                currentSortDirection = currentSortDirection === 'asc' ? 'desc' : 'asc';
            } else {
                currentSortColumn = this;
                currentSortDirection = 'asc';
            }

            // Update visual indicators on all headers
            sortableHeaders.forEach(h => h.classList.remove('sort-asc', 'sort-desc'));
            this.classList.add('sort-' + currentSortDirection);

            // Get all rows (including hidden ones from search) as an array
            const rows = Array.from(tbody.querySelectorAll('tr'));

            rows.sort((rowA, rowB) => {
                const cellA = rowA.cells[columnIndex];
                const cellB = rowB.cells[columnIndex];
                const textA = cellA.textContent.trim();
                const textB = cellB.textContent.trim();

                if (sortType === 'numeric') {
                    const numA = parseFloat(textA) || 0;
                    const numB = parseFloat(textB) || 0;
                    return currentSortDirection === 'asc' ? numA - numB : numB - numA;
                } else {
                    const lowerA = textA.toLowerCase();
                    const lowerB = textB.toLowerCase();
                    return currentSortDirection === 'asc'
                        ? lowerA.localeCompare(lowerB)
                        : lowerB.localeCompare(lowerA);
                }
            });

            // Re-insert sorted rows into the tbody
            rows.forEach(row => tbody.appendChild(row));
        });
    });

});