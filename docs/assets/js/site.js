/* Menu mobile, section active dans le menu, formulaire de contact. Aucune dépendance, aucun appel réseau. */
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
})();
