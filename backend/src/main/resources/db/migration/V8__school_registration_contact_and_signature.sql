ALTER TABLE schools
    ADD COLUMN teacher_in_charge_name VARCHAR(180),
    ADD COLUMN teacher_in_charge_phone VARCHAR(40),
    ADD COLUMN principal_signature_document_id UUID REFERENCES document_uploads(id);
