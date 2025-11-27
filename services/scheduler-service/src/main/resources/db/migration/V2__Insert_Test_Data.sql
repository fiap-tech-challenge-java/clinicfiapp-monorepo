-- Hash BCrypt para a senha "123456"

-- 1. Inserir Usuário MÉDICO (Dr. House)
INSERT INTO users (id, name, email, login, password_hash, role, is_active)
VALUES (
           '4622ecbc-9239-4f7b-8390-c35d02a4e466', -- ID fixo para facilitar
           'Dr. Olhos Bom',
           'olhos@tuamaeaquelaursa.com',
           'olhos', -- Login: medico
           '$2a$12$OEwIDtNGgrAvOOnTnT.8BOXzFq.L1S.GtYeZQa8W/16Py1rdPhxla',
           'doctor',
           true
       );

INSERT INTO doctors (user_id, crm, specialty, is_active)
VALUES ('4622ecbc-9239-4f7b-8390-c35d02a4e466', 'CRM/SP 333456', 'Oftalmologista', true);

-- 1. MÉDICO (Login: medico / Senha: 123456)
INSERT INTO users (id, name, email, login, password_hash, role, is_active)
VALUES (
           '9629005d-47e5-48d1-9791-844dee503faa',
           'Dr. House',
           'house@tuamaeaquelaursa.com',
           'medico',
           '$2a$12$WTIiVLsVYM9trU0aHQfPt.IMapvg3WNxwn2ehZxwCIxtcQvqPGFRu', -- Senha: 123456
           'doctor',
           true
       );

INSERT INTO doctors (user_id, crm, specialty, is_active)
VALUES ('9629005d-47e5-48d1-9791-844dee503faa', 'CRM/SP 123456', 'Diagnostico', true);

-- 2. PACIENTE (Login: paciente / Senha: 123456)
INSERT INTO users (id, name, email, login, password_hash, role, is_active)
VALUES (
           '5c94b27a-1e11-4773-b17e-7ca974a57062',
           'Paciente Teste',
           'paciente@tuamaeaquelaursa.com',
           'paciente',
           '$2a$12$eBvBYaKNc2gmQhJpyuXl4.4BNWgR/ImRmZLyjZbn20Hm2Vbk4irZO', -- Senha: 123456
           'patient',
           true
       );

INSERT INTO patients (user_id, birth_date, is_active)
VALUES ('5c94b27a-1e11-4773-b17e-7ca974a57062', '1990-01-01', true);

-- 3. ENFERMEIRO (Login: enfermeiro / Senha: 123456)
INSERT INTO users (id, name, email, login, password_hash, role, is_active)
VALUES (
           '807f333a-171c-4e7e-92c5-410688af80cd',
           'Enfermeira Joy',
           'joy@tuamaeaquelaursa.com',
           'enfermeiro',
           '$2a$12$3EmB8zopnpRvd6jI8VCE4uWXgXFA7u5dpo6o32CeiOFZhA1n71j0y', -- Senha: 123456
           'nurse',
           true
       );

INSERT INTO nurses (user_id, is_active)
VALUES ('807f333a-171c-4e7e-92c5-410688af80cd', true);