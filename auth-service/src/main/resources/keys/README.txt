Llaves RSA de demostración del auth-service.

- private.pem : llave privada (PKCS#8). Solo la tiene auth-service; firma los JWT (RS256).
- public.pem  : llave pública (X.509). Se usa para validar los JWT.

Para regenerarlas ejecute desde la raíz del proyecto (Git Bash):

    sh scripts/generate-rsa-keys.sh

Tras regenerarlas, los tokens emitidos antes dejan de ser válidos: vuelva a iniciar sesión.
Uso académico: no reutilice estas llaves en ambientes productivos.
