CREATE DATABASE sistema_bancario;
USE sistema_bancario;

-- creacion de tablas de la base de datos

CREATE TABLE clientes (
    dui_cliente VARCHAR(10) NOT NULL,
    nombre_completo_cliente VARCHAR(250) NOT NULL,
    correo_cliente VARCHAR(175) NOT NULL,
    direccion_cliente VARCHAR(250) NOT NULL,
    estado_cliente VARCHAR(10) NOT NULL
);

CREATE TABLE telefonos_clientes (
	id_telefono INT NOT NULL,
    dui_cliente VARCHAR(10) NOT NULL,
    telefono_cliente VARCHAR(20) NOT NULL
);

CREATE TABLE cuentas (
    numero_cuenta VARCHAR(25) NOT NULL,
    dui_cliente VARCHAR(10) NOT NULL,
    tipo_cuenta VARCHAR(15) NOT NULL,
    saldo DECIMAL(12,2) NOT NULL,
    fecha_apertura DATE NOT NULL,
    estado_cuenta VARCHAR(10) NOT NULL
);

-- asignacion de llaves primarias PK

ALTER TABLE clientes ADD CONSTRAINT PK_clientes PRIMARY KEY (dui_cliente);

ALTER TABLE telefonos_clientes ADD CONSTRAINT PK_telefonos_clientes PRIMARY KEY (id_telefono);

-- id_telefono se genera automaticamente al registrar un telefono
ALTER TABLE telefonos_clientes MODIFY id_telefono INT NOT NULL AUTO_INCREMENT;

ALTER TABLE cuentas ADD CONSTRAINT PK_cuentas PRIMARY KEY (numero_cuenta);


-- asignacion de llaves foraneas FK

-- FK de telefonos_clientes
ALTER TABLE telefonos_clientes ADD CONSTRAINT FK_telefonos_clientes FOREIGN KEY (dui_cliente) REFERENCES clientes(dui_cliente);

-- FK de cuentas
ALTER TABLE cuentas ADD CONSTRAINT FK_cuentas_clientes FOREIGN KEY (dui_cliente) REFERENCES clientes(dui_cliente);

-- asignacion de restricciones de datos

ALTER TABLE cuentas
ADD CONSTRAINT CHK_tipo_cuenta
CHECK (tipo_cuenta IN ('AHORRO', 'CORRIENTE'));

ALTER TABLE cuentas
ADD CONSTRAINT CHK_estado_cuenta
CHECK (estado_cuenta IN ('ACTIVA', 'INACTIVA', 'BLOQUEADA'));

ALTER TABLE cuentas
ADD CONSTRAINT CHK_saldo
CHECK (saldo >= 0);

-- insersiones de datos

INSERT INTO clientes (
    dui_cliente,
    nombre_completo_cliente,
    correo_cliente,
    direccion_cliente,
    estado_cliente
)
VALUES
('01234567-8', 'Carlos Hernandez', 'carlos@gmail.com', 'San Salvador', 'ACTIVO'),
('02345678-9', 'Maria Lopez', 'maria@gmail.com', 'Soyapango', 'ACTIVO'),
('03456789-0', 'Jose Martinez', 'jose@gmail.com', 'Santa Ana', 'INACTIVO'),
('04567890-1', 'Ana Garcia', 'ana@gmail.com', 'San Miguel', 'BLOQUEADO'),
('05678901-2', 'Luis Ramirez', 'luis@gmail.com', 'Sonsonate', 'ACTIVO');

SELECT * FROM clientes;

INSERT INTO telefonos_clientes (
    id_telefono,
    dui_cliente,
    telefono_cliente
)
VALUES
(101, '01234567-8', '70123456'),
(102, '01234567-8', '71123456'),
(103, '02345678-9', '72123456'),
(104, '03456789-0', '73123456'),
(105, '04567890-1', '74123456'),
(106, '04567890-1', '75123456'),
(107, '05678901-2', '76123456');

SELECT * FROM telefonos_clientes;

INSERT INTO cuentas (
    numero_cuenta,
    dui_cliente,
    tipo_cuenta,
    saldo,
    fecha_apertura,
    estado_cuenta
)
VALUES
('CTA-000001', '01234567-8', 'AHORRO', 500.00, '2026-11-01', 'ACTIVA'),
('CTA-000002', '01234567-8', 'CORRIENTE', 1200.50, '2026-11-02', 'ACTIVA'),
('CTA-000003', '02345678-9', 'AHORRO', 850.75, '2026-11-02', 'ACTIVA'),
('CTA-000004', '03456789-0', 'CORRIENTE', 300.00, '2026-11-03', 'INACTIVA'),
('CTA-000005', '05678901-2', 'AHORRO', 1500.00, '2026-11-04', 'ACTIVA');

SELECT * FROM cuentas;



