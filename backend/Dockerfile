FROM node:24-bookworm-slim
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev --ignore-scripts && npm cache clean --force
COPY src ./src
COPY migrations ./migrations
RUN mkdir -p /app/var/uploads && chown -R node:node /app/var
USER node
ENV NODE_ENV=production MATCHORA_ENV=production HOST=0.0.0.0 PORT=8787
EXPOSE 8787
HEALTHCHECK --interval=30s --timeout=5s CMD node -e "fetch('http://127.0.0.1:8787/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node", "src/server.mjs"]
