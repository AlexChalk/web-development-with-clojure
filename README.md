# Guestbook

A simple guestbook application. Updates are made over a websocket collection, so users will see new posts appear in real-time.

### Setup

```
lein run migrate
lein cljsbuild once
```

### Running
```
lein run
```
Port defaults to http://localhost:3000

### Tests

```
lein test
```

### Other Notes

Built from a tutorial in  Dmitri Sotnikov's book [Web Development with Clojure](https://pragprog.com/book/dswdcloj/web-development-with-clojure).