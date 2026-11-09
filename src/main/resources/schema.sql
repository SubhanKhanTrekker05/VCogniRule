CREATE TABLE IF NOT EXISTS admin (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    field_key TEXT NOT NULL UNIQUE,
    text TEXT NOT NULL,
    category TEXT NOT NULL CHECK(category IN ('MOOD','ANHEDONIA','SLEEP','ANXIETY','COGNITION','FUNCTIONING','RISK')),
    display_order INTEGER NOT NULL,
    instrument TEXT NOT NULL,
    scale_type TEXT NOT NULL DEFAULT 'LIKERT_03',
    default_value INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS rule (
    id INTEGER PRIMARY KEY,
    rule_id TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    level INTEGER NOT NULL DEFAULT 1 CHECK(level IN (1,2,3,4)),
    category TEXT NOT NULL CHECK(category IN ('MOOD','ANHEDONIA','SLEEP','ANXIETY','COGNITION','FUNCTIONING','RISK','CROSS_CUTTING')),
    priority INTEGER NOT NULL DEFAULT 50 CHECK(priority BETWEEN 0 AND 100),
    action_type TEXT NOT NULL CHECK(action_type IN ('DISPLAY_INFO','MONITOR','SUGGEST_PROFESSIONAL','RECOMMEND_ASSESSMENT','URGENT_ASSESSMENT','CRISIS_PROTOCOL')),
    explanation TEXT,
    source_ref TEXT,
    conclusion_key TEXT,
    conclusion_value TEXT,
    is_active INTEGER NOT NULL DEFAULT 1 CHECK(is_active IN (0, 1)),
    version INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rule_condition_group (
    id INTEGER PRIMARY KEY,
    rule_id INTEGER NOT NULL,
    logic TEXT NOT NULL DEFAULT 'AND',
    FOREIGN KEY (rule_id) REFERENCES rule(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rule_condition (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id INTEGER NOT NULL,
    field_key TEXT NOT NULL,
    operator TEXT NOT NULL CHECK(operator IN ('EQ','NE','GT','GTE','LT','LTE','BETWEEN','IN')),
    value_int INTEGER,
    value_text TEXT,
    value_min INTEGER,
    value_max INTEGER,
    FOREIGN KEY (group_id) REFERENCES rule_condition_group(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_question_order ON question(display_order);
CREATE INDEX IF NOT EXISTS idx_rule_active ON rule(is_active);
CREATE INDEX IF NOT EXISTS idx_rule_level ON rule(level);
CREATE INDEX IF NOT EXISTS idx_condition_group ON rule_condition(group_id);