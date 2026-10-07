/* Mobile menu, active section in the menu, contact form, screenshot slideshow. No dependency, no network call. */
(function () {
    "use strict";

    // Menu mobile
    var toggle = document.querySelector(".menu-toggle");
    var nav = document.getElementById("nav");
    if (toggle && nav) {
        toggle.addEventListener("click", function () {
            var open = nav.classList.toggle("is-open");
            toggle.setAttribute("aria-expanded", open ? "true" : "false");
        });
        nav.addEventListener("click", function (event) {
            if (event.target.closest("a")) {
                nav.classList.remove("is-open");
                toggle.setAttribute("aria-expanded", "false");
            }
        });
    }

    // Section visible = entrée du menu en surbrillance
    var links = Array.prototype.slice.call(document.querySelectorAll('.nav a[href^="#"]'));
    if ("IntersectionObserver" in window && links.length) {
        var byId = {};
        links.forEach(function (link) {
            byId[link.getAttribute("href").slice(1)] = link;
        });
        var observer = new IntersectionObserver(
            function (entries) {
                entries.forEach(function (entry) {
                    if (!entry.isIntersecting) return;
                    links.forEach(function (link) { link.classList.remove("is-active"); });
                    var link = byId[entry.target.id];
                    if (link) link.classList.add("is-active");
                });
            },
            { rootMargin: "-45% 0px -50% 0px" }
        );
        Object.keys(byId).forEach(function (id) {
            var section = document.getElementById(id);
            if (section) observer.observe(section);
        });
    }

    // Formulaire de contact : site statique, donc pas de serveur. Le message est préparé dans le logiciel de messagerie de
    // l'utilisateur (lien mailto:), qui l'envoie lui-même : rien ne transite par un service tiers.
    var form = document.getElementById("contact-form");
    if (form) {
        form.addEventListener("submit", function (event) {
            event.preventDefault();
            if (form.elements["website"] && form.elements["website"].value) return; // anti-robot : champ caché rempli
            if (!form.reportValidity()) return;

            var data = form.dataset;
            var name = form.elements["name"].value.trim();
            var from = form.elements["email"].value.trim();
            var message = form.elements["message"].value.trim();
            var subject = data.subject + (name ? " — " + name : "");
            var body = message + "\n\n— " + data.labelName + " : " + name + "\n— " + data.labelEmail + " : " + from;

            window.location.href =
                "mailto:" + data.email + "?subject=" + encodeURIComponent(subject) + "&body=" + encodeURIComponent(body);

            var note = document.getElementById("form-status");
            if (note) note.textContent = data.sentNote;
        });
    }

    // Screenshot viewer: clicking a phone opens the screenshots as a slideshow (arrows, swipe, keyboard, optional autoplay).
    var texts = {
        en: { open: "Enlarge the screenshot", close: "Close", prev: "Previous screenshot", next: "Next screenshot",
              play: "Start the slideshow", pause: "Pause the slideshow", of: "of", label: "Screenshots" },
        fr: { open: "Agrandir la capture", close: "Fermer", prev: "Capture précédente", next: "Capture suivante",
              play: "Lancer le diaporama", pause: "Mettre le diaporama en pause", of: "sur", label: "Captures d’écran" }
    };
    var t = texts[(document.documentElement.lang || "en").slice(0, 2)] || texts.en;

    var images = Array.prototype.slice.call(document.querySelectorAll(".phone img"));
    if (images.length) {
        // One slide per distinct screenshot; the caption comes from the first figure that has one.
        // The gallery ("A look inside") comes first, in its own order: it tells the story from the welcome screen to the settings.
        var inGallery = images.filter(function (img) { return img.closest(".gallery"); });
        var ordered = inGallery.concat(images.filter(function (img) { return inGallery.indexOf(img) < 0; }));
        var slides = [];
        var indexBySrc = {};
        ordered.forEach(function (img) {
            var figure = img.closest("figure");
            var caption = figure && figure.querySelector("figcaption") ? figure.querySelector("figcaption").textContent : "";
            var src = img.getAttribute("src");
            if (!(src in indexBySrc)) {
                indexBySrc[src] = slides.length;
                slides.push({ src: src, alt: img.getAttribute("alt") || "", caption: caption });
            } else if (caption && !slides[indexBySrc[src]].caption) {
                slides[indexBySrc[src]].caption = caption;
            }
        });

        var icon = function (path) {
            return '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" ' +
                'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + path + "</svg>";
        };
        var ICON_PREV = icon('<path d="M15 5l-7 7 7 7"/>');
        var ICON_NEXT = icon('<path d="M9 5l7 7-7 7"/>');
        var ICON_CLOSE = icon('<path d="M6 6l12 12M18 6L6 18"/>');
        var ICON_PLAY = icon('<path d="M8 5l11 7-11 7z" fill="currentColor"/>');
        var ICON_PAUSE = icon('<path d="M8 5v14M16 5v14"/>');

        var viewer = document.createElement("div");
        viewer.className = "viewer";
        viewer.hidden = true;
        viewer.setAttribute("role", "dialog");
        viewer.setAttribute("aria-modal", "true");
        viewer.setAttribute("aria-label", t.label);
        viewer.innerHTML =
            '<div class="viewer-bar"><span class="viewer-count" aria-live="polite"></span><span class="viewer-tools">' +
            '<button type="button" class="viewer-btn viewer-play"></button>' +
            '<button type="button" class="viewer-btn viewer-close"></button></span></div>' +
            '<div class="viewer-stage"><button type="button" class="viewer-btn viewer-nav viewer-prev"></button>' +
            '<figure class="viewer-figure"><img alt=""><figcaption></figcaption></figure>' +
            '<button type="button" class="viewer-btn viewer-nav viewer-next"></button></div>';
        document.body.appendChild(viewer);

        var big = viewer.querySelector("img");
        var captionEl = viewer.querySelector("figcaption");
        var countEl = viewer.querySelector(".viewer-count");
        var playBtn = viewer.querySelector(".viewer-play");
        var closeBtn = viewer.querySelector(".viewer-close");
        var prevBtn = viewer.querySelector(".viewer-prev");
        var nextBtn = viewer.querySelector(".viewer-next");
        prevBtn.innerHTML = ICON_PREV;
        nextBtn.innerHTML = ICON_NEXT;
        closeBtn.innerHTML = ICON_CLOSE;
        prevBtn.setAttribute("aria-label", t.prev);
        nextBtn.setAttribute("aria-label", t.next);
        closeBtn.setAttribute("aria-label", t.close);

        var current = 0;
        var opener = null;
        var timer = null;

        function show(index) {
            current = (index + slides.length) % slides.length;
            var slide = slides[current];
            big.src = slide.src;
            big.alt = slide.alt;
            captionEl.textContent = slide.caption;
            countEl.textContent = current + 1 + " " + t.of + " " + slides.length;
            [current - 1, current + 1].forEach(function (n) { // warm the cache for the neighbours
                new Image().src = slides[(n + slides.length) % slides.length].src;
            });
        }

        function setPlaying(on) {
            clearInterval(timer);
            timer = on ? setInterval(function () { show(current + 1); }, 3500) : null;
            playBtn.innerHTML = on ? ICON_PAUSE : ICON_PLAY;
            playBtn.setAttribute("aria-label", on ? t.pause : t.play);
            playBtn.setAttribute("aria-pressed", on ? "true" : "false");
        }

        function go(step) {
            show(current + step);
            if (timer) setPlaying(true); // restart the delay after a manual move
        }

        function open(index, trigger) {
            opener = trigger;
            setPlaying(false);
            show(index);
            viewer.hidden = false;
            document.body.classList.add("viewer-open");
            closeBtn.focus();
        }

        function close() {
            setPlaying(false);
            viewer.hidden = true;
            document.body.classList.remove("viewer-open");
            if (opener) opener.focus();
        }

        prevBtn.addEventListener("click", function () { go(-1); });
        nextBtn.addEventListener("click", function () { go(1); });
        closeBtn.addEventListener("click", close);
        playBtn.addEventListener("click", function () { setPlaying(!timer); });
        viewer.addEventListener("click", function (event) {
            // a click on the dark background (not on the picture or a button) closes the viewer
            if (event.target === viewer || event.target.classList.contains("viewer-stage")) close();
        });

        document.addEventListener("keydown", function (event) {
            if (viewer.hidden) return;
            if (event.key === "Escape") {
                close();
            } else if (event.key === "ArrowLeft") {
                go(-1);
            } else if (event.key === "ArrowRight") {
                go(1);
            } else if (event.key === "Home") {
                show(0);
            } else if (event.key === "End") {
                show(slides.length - 1);
            } else if (event.key === "Tab") { // keep the focus inside the dialog
                var buttons = [playBtn, closeBtn, prevBtn, nextBtn];
                var at = buttons.indexOf(document.activeElement);
                var next = event.shiftKey ? at - 1 : at + 1;
                buttons[(next + buttons.length) % buttons.length].focus();
            } else {
                return;
            }
            event.preventDefault();
        });

        var touchX = 0;
        var touchY = 0;
        viewer.addEventListener("touchstart", function (event) {
            touchX = event.changedTouches[0].clientX;
            touchY = event.changedTouches[0].clientY;
        }, { passive: true });
        viewer.addEventListener("touchend", function (event) {
            var dx = event.changedTouches[0].clientX - touchX;
            var dy = event.changedTouches[0].clientY - touchY;
            if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy)) go(dx < 0 ? 1 : -1);
        }, { passive: true });

        images.forEach(function (img) {
            var phone = img.closest(".phone");
            if (!phone) return;
            var index = indexBySrc[img.getAttribute("src")];
            phone.setAttribute("role", "button");
            phone.setAttribute("tabindex", "0");
            phone.setAttribute("aria-label", t.open + ": " + (slides[index].caption || slides[index].alt));
            phone.classList.add("is-zoomable");
            phone.addEventListener("click", function () { open(index, phone); });
            phone.addEventListener("keydown", function (event) {
                if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    open(index, phone);
                }
            });
        });
    }
})();
