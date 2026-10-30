CREATE TABLE tasks (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(150)  NOT NULL,
    description VARCHAR(2000) NOT NULL,
    due_date    DATE          NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    owner_id    BIGINT        NOT NULL,
    CONSTRAINT fk_tasks_owner
        FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT ck_tasks_status CHECK (status IN ('TODO', 'DONE'))
);
CREATE INDEX idx_tasks_owner_due ON tasks(owner_id, due_date);

