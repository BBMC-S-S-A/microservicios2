Llave pública RSA de demostración del user-service.

- public.pem : llave pública (X.509). user-service solo VALIDA JWT; nunca debe tener private.pem.

Debe ser la misma public.pem que usa auth-service. Para regenerar el par ejecute desde la raíz:

    sh scripts/generate-rsa-keys.sh

Uso académico: no reutilice estas llaves en ambientes productivos.
