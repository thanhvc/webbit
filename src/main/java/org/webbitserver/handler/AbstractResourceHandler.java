package org.webbitserver.handler;

import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public abstract class AbstractResourceHandler implements HttpHandler {
    static {
        // This is not an exhaustive list, just the most common types. Call registerMimeType() to add more.
        Map<String, String> mimeTypes = new HashMap<String, String>();
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("csv", "text/csv");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("xml", "text/xml");
        mimeTypes.put("js", "text/javascript"); // Technically it should be application/javascript (RFC 4329), but IE8 struggles with that
        mimeTypes.put("xhtml", "application/xhtml+xml");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("zip", "application/zip");
        mimeTypes.put("tar", "application/x-tar");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("tiff", "image/tiff");
        mimeTypes.put("tif", "image/tiff");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("svg", "image/svg+xml");
        mimeTypes.put("ico", "image/vnd.microsoft.icon");
        DEFAULT_MIME_TYPES = Collections.unmodifiableMap(mimeTypes);
    }

    public static final Map<String, String> DEFAULT_MIME_TYPES;
    protected static final String DEFAULT_WELCOME_FILE_NAME = "index.html";
    protected final Executor ioThread;
    protected final Map<String, String> mimeTypes;
    protected String welcomeFileName;

    public AbstractResourceHandler(Executor ioThread) {
        this.ioThread = ioThread;
        this.mimeTypes = new HashMap<String, String>(DEFAULT_MIME_TYPES);
        this.welcomeFileName = DEFAULT_WELCOME_FILE_NAME;
    }

    public AbstractResourceHandler addMimeType(String extension, String mimeType) {
        mimeTypes.put(extension, mimeType);
        return this;
    }

    public AbstractResourceHandler welcomeFile(String welcomeFile) {
        this.welcomeFileName = welcomeFile;
        return this;
    }

    @Override
    public void handleHttpRequest(final HttpRequest request, final HttpResponse response, final HttpControl control) throws Exception {
        // Switch from web thead to IO thread, so we don't block web server when we access the filesystem.
        ioThread.execute(createIOWorker(request, response, control));
    }

    protected abstract StaticFileHandler.IOWorker createIOWorker(HttpRequest request, HttpResponse response, HttpControl control);

    /**
     * All IO is performed by this worker on a separate thread, so we never block the HttpHandler.
     */
    protected abstract class IOWorker implements Runnable {

        protected String path;
        protected final HttpResponse response;
        protected final HttpControl control;

        protected IOWorker(String path, HttpResponse response, HttpControl control) {
            this.path = path;
            this.response = response;
            this.control = control;
        }

        protected void notFound() {
            // Switch back from IO thread to web thread.
            control.execute(new Runnable() {
                @Override
                public void run() {
                    control.nextHandler();
                }
            });
        }

        protected void serve(final String mimeType, final byte[] contents) {
            // Switch back from IO thread to web thread.
            control.execute(new Runnable() {
                @Override
                public void run() {
                    // TODO: Don't read all into memory, instead use zero-copy.
                    // TODO: Check bytes read match expected encoding of mime-type
                    response.header("Content-Type", mimeType)
                            .header("Content-Length", contents.length)
                            .content(contents)
                            .end();
                }
            });
        }

        protected void error(final IOException exception) {
            // Switch back from IO thread to web thread.
            control.execute(new Runnable() {
                @Override
                public void run() {
                    response.error(exception);
                }
            });
        }

        @Override
        public void run() {
            path = withoutTrailingSlashOrQuery(path);

            // TODO: Cache
            // TODO: If serving directory and trailing slash omitted, perform redirect
            try {
                byte[] content = null;
                if (!exists()) {
                    notFound();
                } else if ((content = fileBytes()) != null) {
                    serve(guessMimeType(path), content);
                } else {
                    if ((content = welcomeBytes()) != null) {
                        serve(guessMimeType(welcomeFileName), content);
                    } else {
                        notFound();
                    }
                }
            } catch (IOException e) {
                error(e);
            }
        }

        protected abstract boolean exists() throws IOException;

        protected abstract byte[] fileBytes() throws IOException;

        protected abstract byte[] welcomeBytes() throws IOException;

        protected byte[] read(int length, InputStream in) throws IOException {
            byte[] data = new byte[length];
            try {
                int read = 0;
                while (read < length) {
                    int more = in.read(data, read, data.length - read);
                    if(more == -1) {
                        break;
                    } else {
                        read += more;
                    }
                }
            } finally {
                in.close();
            }
            return data;
        }

        // TODO: Don't respond with a mime type that violates the request's Accept header
        private String guessMimeType(String path) {
            int lastDot = path.lastIndexOf('.');
            if (lastDot == -1) {
                return null;
            }
            String extension = path.substring(lastDot + 1).toLowerCase();
            String mimeType = mimeTypes.get(extension);
            if (mimeType == null) {
                return null;
            }
            if (mimeType.startsWith("text/") && response.charset() != null) {
                mimeType += "; charset=" + response.charset().name();
            }
            return mimeType;
        }

        protected String withoutTrailingSlashOrQuery(String path) {
            int queryStart = path.indexOf('?');
            if (queryStart > -1) {
                path = path.substring(0, queryStart);
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return path;
        }

    }
}
