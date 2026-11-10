FROM quay.io/keycloak/keycloak:26.0
COPY realm-export.json /opt/keycloak/data/import/realm-export.json
# /auth prefix so all auth URLs live under one nginx-proxied path
ENV KC_HTTP_RELATIVE_PATH=/auth
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev", "--import-realm"]
