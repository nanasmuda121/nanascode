package com.nanascode.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val files: Map<String, String> = emptyMap()
) {
    companion object {
        val ALL_TEMPLATES: List<ProjectTemplate> = listOf(
            ProjectTemplate(
                id = "empty",
                name = "Proyek Kosong",
                description = "Mulai dari awal dengan workspace kosong",
                iconName = "folder",
                files = mapOf(
                    "README.md" to "# Proyek Baru\n\nDibuat dengan NasCode Mobile Editor\n",
                    ".gitignore" to ".DS_Store\nnode_modules/\n*.log\n"
                )
            ),
            ProjectTemplate(
                id = "html-tailwind",
                name = "HTML5 + Tailwind CSS v4",
                description = "Template frontend modern dengan styling Tailwind CSS v4",
                iconName = "brush",
                files = mapOf(
                    "index.html" to """
                        <!DOCTYPE html>
                        <html lang="id">
                        <head>
                          <meta charset="UTF-8">
                          <meta name="viewport" content="width=device-width, initial-scale=1.0">
                          <title>NasCode App</title>
                          <link rel="stylesheet" href="style.css">
                        </head>
                        <body class="bg-zinc-900 text-white min-h-screen flex items-center justify-center p-4">
                          <div class="max-w-md w-full bg-zinc-800 border border-zinc-700 rounded-2xl p-6 text-center shadow-2xl">
                            <h1 class="text-3xl font-extrabold text-white mb-2">NasCode</h1>
                            <p class="text-zinc-400 mb-4">Mobile Code Editor siap koding di Android.</p>
                            <button class="bg-amber-400 hover:bg-amber-300 text-black font-semibold px-4 py-2 rounded-xl transition">
                              Mulai Koding
                            </button>
                          </div>
                        </body>
                        </html>
                    """.trimIndent(),
                    "style.css" to "@import \"tailwindcss\";\n",
                    "package.json" to """
                        {
                          "name": "tailwind-nanas",
                          "version": "1.0.0",
                          "scripts": {
                            "build": "npx @tailwindcss/cli -i style.css -o dist/output.css",
                            "dev": "npx @tailwindcss/cli -i style.css -o dist/output.css --watch"
                          },
                          "devDependencies": {
                            "@tailwindcss/cli": "^4.0.0",
                            "tailwindcss": "^4.0.0"
                          }
                        }
                    """.trimIndent()
                )
            ),
            ProjectTemplate(
                id = "nodejs-express",
                name = "Node.js + Express API",
                description = "Backend REST API ringan berbasis Node.js",
                iconName = "code",
                files = mapOf(
                    "index.js" to """
                        const express = require('express');
                        const app = express();
                        const PORT = process.env.PORT || 3000;

                        app.use(express.json());

                        app.get('/', (req, res) => {
                          res.json({
                            status: 'success',
                            message: 'Halo dari server NasCode di Android!',
                            time: new Date().toISOString()
                          });
                        });

                        app.listen(PORT, () => {
                          console.log(`Server aktif di http://localhost:${'$'}{PORT}`);
                        });
                    """.trimIndent(),
                    "package.json" to """
                        {
                          "name": "nanas-express-api",
                          "version": "1.0.0",
                          "main": "index.js",
                          "scripts": {
                            "start": "node index.js"
                          },
                          "dependencies": {
                            "express": "^4.19.2"
                          }
                        }
                    """.trimIndent(),
                    ".gitignore" to "node_modules/\n.env\n"
                )
            ),
            ProjectTemplate(
                id = "react-vite",
                name = "React + Vite SPA",
                description = "Aplikasi Single Page modern menggunakan React",
                iconName = "web",
                files = mapOf(
                    "index.html" to """
                        <!doctype html>
                        <html lang="en">
                          <head>
                            <meta charset="UTF-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                            <title>Nanas React App</title>
                          </head>
                          <body>
                            <div id="root"></div>
                            <script type="module" src="/src/main.jsx"></script>
                          </body>
                        </html>
                    """.trimIndent(),
                    "src/main.jsx" to """
                        import React from 'react';
                        import ReactDOM from 'react-dom/client';
                        import App from './App.jsx';

                        ReactDOM.createRoot(document.getElementById('root')).render(
                          <React.StrictMode>
                            <App />
                          </React.StrictMode>
                        );
                    """.trimIndent(),
                    "src/App.jsx" to """
                        import React, { useState } from 'react';

                        export default function App() {
                          const [count, setCount] = useState(0);

                          return (
                            <div style={{ fontFamily: 'sans-serif', padding: '2rem', textAlign: 'center' }}>
                              <h1>NasCode React App</h1>
                              <p>Edit <code>src/App.jsx</code> langsung dari HP Android!</p>
                              <button onClick={() => setCount(count + 1)} style={{ padding: '8px 16px', fontSize: '1rem' }}>
                                Diklik: {count} kali
                              </button>
                            </div>
                          );
                        }
                    """.trimIndent(),
                    "package.json" to """
                        {
                          "name": "nascode-react-vite",
                          "private": true,
                          "version": "0.0.0",
                          "type": "module",
                          "scripts": {
                            "dev": "vite",
                            "build": "vite build",
                            "preview": "vite preview"
                          },
                          "dependencies": {
                            "react": "^18.3.1",
                            "react-dom": "^18.3.1"
                          },
                          "devDependencies": {
                            "@vitejs/plugin-react": "^4.3.1",
                            "vite": "^5.3.4"
                          }
                        }
                    """.trimIndent()
                )
            ),
            ProjectTemplate(
                id = "python-cli",
                name = "Python CLI Script",
                description = "Skrip Python sederhana untuk automasi atau data",
                iconName = "terminal",
                files = mapOf(
                    "main.py" to """
                        #!/usr/bin/env python3
                        import sys

                        def main():
                            print("Halo dari Python di NasCode Android!")
                            print(f"Versi Python: {sys.version}")

                        if __name__ == "__main__":
                            main()
                    """.trimIndent(),
                    "requirements.txt" to "# Tambahkan package pip di sini\n",
                    "README.md" to "# Python Project\n\nJalankan dengan `python3 main.py` di terminal NanasCode.\n"
                )
            )
        )
    }
}
