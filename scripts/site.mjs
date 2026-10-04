#!/usr/bin/env node
// Outils du site statique (docs/) : mise à jour de la FAQ depuis l'application, vérifications, adresse de contact.
//
//   node scripts/site.mjs sync-faq          recopie la FAQ de l'application (res/raw/faq.xml, res/raw-fr/faq.xml) dans les pages
//   node scripts/site.mjs check             vérifie les pages (ancres, liens, images, langues, FAQ à jour, adresse de contact)
//   node scripts/site.mjs set-email <adr>   remplace l'adresse de contact dans toutes les pages
import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const docs = join(root, "docs");
const langs = {
    en: { xml: "app/src/main/res/raw/faq.xml", questions: (n) => (n === 1 ? "1 question" : `${n} questions`) },
    fr: { xml: "app/src/main/res/raw-fr/faq.xml", questions: (n) => (n === 1 ? "1 question" : `${n} questions`) },
};
const START = "<!-- faq:start -->";
const END = "<!-- faq:end -->";

const read = (path) => readFileSync(path, "utf8");
// L'anglais est à la racine du site (docs/index.html), le français dans docs/fr/.
const page = (lang) => (lang === "en" ? join(docs, "index.html") : join(docs, lang, "index.html"));

const decode = (text) =>
    text.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&amp;/g, "&");
const escapeHtml = (text) => text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");

/** Lit la FAQ de l'application (même format que FaqParser.kt : thèmes, questions, réponses en paragraphes). */
function parseFaq(rawXml) {
    const xml = rawXml.replace(/<!--[\s\S]*?-->/g, ""); // le commentaire d'en-tête cite des balises d'exemple
    const themes = [];
    for (const theme of xml.matchAll(/<theme\s+id="([^"]+)"\s+title="([^"]*)"\s*>([\s\S]*?)<\/theme>/g)) {
        const entries = [];
        for (const entry of theme[3].matchAll(/<entry\s+id="([^"]+)"\s*>([\s\S]*?)<\/entry>/g)) {
            const question = /<question>([\s\S]*?)<\/question>/.exec(entry[2]);
            const answer = /<answer>([\s\S]*?)<\/answer>/.exec(entry[2]);
            if (!question || !answer) throw new Error(`Question incomplète : ${theme[1]}/${entry[1]}`);
            const paragraphs = decode(answer[1])
                .trim()
                .split(/\n\s*\n/)
                .map((p) => p.split("\n").map((line) => line.trim()).filter(Boolean).join(" "))
                .filter(Boolean);
            entries.push({ id: entry[1], question: decode(question[1]).trim().replace(/\s+/g, " "), paragraphs });
        }
        themes.push({ id: theme[1], title: decode(theme[2]), entries });
    }
    if (!themes.length) throw new Error("Aucun thème dans la FAQ");
    const declared = (xml.match(/<theme[\s>]/g) || []).length;
    if (declared !== themes.length) throw new Error(`FAQ : ${declared} thèmes déclarés, ${themes.length} lus`);
    return themes;
}

function faqHtml(lang) {
    const themes = parseFaq(read(join(root, langs[lang].xml)));
    return themes
        .map((theme) => {
            const questions = theme.entries
                .map(
                    (entry) =>
                        `                        <details class="question" id="faq-${theme.id}-${entry.id}">\n` +
                        `                            <summary>${escapeHtml(entry.question)}</summary>\n` +
                        `                            <div class="answer">${entry.paragraphs.map((p) => `<p>${escapeHtml(p)}</p>`).join("")}</div>\n` +
                        `                        </details>`,
                )
                .join("\n");
            return (
                `                    <details class="theme" id="faq-${theme.id}">\n` +
                `                        <summary><span>${escapeHtml(theme.title)}<small>${langs[lang].questions(theme.entries.length)}</small></span></summary>\n` +
                `                        <div class="questions">\n${questions}\n                        </div>\n` +
                `                    </details>`
            );
        })
        .join("\n");
}

/** La page avec son bloc FAQ à jour. */
function withFaq(lang) {
    const html = read(page(lang));
    const start = html.indexOf(START);
    const end = html.indexOf(END);
    if (start < 0 || end < start) throw new Error(`${page(lang)} : repères ${START} / ${END} introuvables`);
    return html.slice(0, start + START.length) + "\n" + faqHtml(lang) + "\n" + html.slice(end);
}

function syncFaq() {
    for (const lang of Object.keys(langs)) {
        writeFileSync(page(lang), withFaq(lang));
        console.log(`FAQ ${lang} mise à jour`);
    }
}

function check() {
    const problems = [];
    const fail = (message) => problems.push(message);
    const sections = {};
    const emails = new Set();

    for (const file of [page("en"), page("fr")]) {
        if (!existsSync(file)) fail(`page manquante : ${file}`);
    }
    if (problems.length) return report(problems);

    for (const lang of Object.keys(langs)) {
        const html = read(page(lang));
        const where = page(lang).slice(root.length + 1).replaceAll("\\", "/");
        if (!html.includes(`<html lang="${lang}">`)) fail(`${where} : attribut lang absent ou faux`);
        if (/lorem ipsum/i.test(html)) fail(`${where} : texte provisoire (Lorem Ipsum)`);

        // Ancres : chaque href="#x" mène à un id existant ; les ids sont uniques.
        const ids = [...html.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]);
        const duplicates = ids.filter((id, i) => ids.indexOf(id) !== i);
        if (duplicates.length) fail(`${where} : ids en double : ${[...new Set(duplicates)].join(", ")}`);
        for (const [, anchor] of html.matchAll(/href="#([^"]*)"/g)) {
            if (anchor && !ids.includes(anchor)) fail(`${where} : ancre #${anchor} sans section correspondante`);
        }
        sections[lang] = [...html.matchAll(/<section[^>]*\sid="([^"]+)"/g)].map((m) => m[1]);

        // Fichiers locaux (images, styles, scripts, autre langue) : ils doivent exister.
        for (const [, ref] of html.matchAll(/(?:src|href)="([^"#?]+)"/g)) {
            if (/^(https?:|mailto:|data:)/.test(ref)) continue;
            const target = join(dirname(page(lang)), ref);
            if (!existsSync(target)) fail(`${where} : fichier introuvable : ${ref}`);
        }

        // Accessibilité : toute capture a un texte alternatif détaillé.
        for (const [, tag] of html.matchAll(/<img\s([^>]*assets\/img[^>]*)>/g)) {
            const alt = /\salt="([^"]*)"/.exec(" " + tag);
            if (!alt || alt[1].trim().length < 10) fail(`${where} : image sans texte alternatif : ${tag.slice(0, 70)}…`);
        }

        // FAQ identique à celle de l'application.
        if (html !== withFaq(lang)) fail(`${where} : la FAQ n'est pas à jour (node scripts/site.mjs sync-faq)`);

        for (const [, email] of html.matchAll(/data-email="([^"]+)"/g)) emails.add(email);
        for (const [, email] of html.matchAll(/mailto:([^"?]+)/g)) emails.add(email);
    }

    const faqIds = (lang) => parseFaq(read(join(root, langs[lang].xml))).map((theme) => `${theme.id}:${theme.entries.map((e) => e.id)}`);
    if (JSON.stringify(faqIds("en")) !== JSON.stringify(faqIds("fr"))) fail("la FAQ n'a pas les mêmes thèmes et questions en anglais et en français");
    if (JSON.stringify(sections.en) !== JSON.stringify(sections.fr)) {
        fail(`sections différentes entre les langues : en = ${sections.en} / fr = ${sections.fr}`);
    }
    if (emails.size !== 1) fail(`adresse de contact incohérente : ${[...emails].join(", ")}`);
    report(problems);
}

function report(problems) {
    if (problems.length) {
        console.error(problems.map((p) => `✗ ${p}`).join("\n"));
        process.exit(1);
    }
    console.log("site : OK");
}

function setEmail(address) {
    if (!/^[^\s@"<>]+@[^\s@"<>]+\.[^\s@"<>]+$/.test(address ?? "")) {
        console.error("Usage : node scripts/site.mjs set-email adresse@exemple.fr");
        process.exit(2);
    }
    for (const lang of Object.keys(langs)) {
        const html = read(page(lang));
        const current = /data-email="([^"]+)"/.exec(html)?.[1];
        if (!current) throw new Error(`${page(lang)} : adresse actuelle introuvable`);
        writeFileSync(page(lang), html.split(current).join(address));
        console.log(`${lang} : ${current} → ${address}`);
    }
}

const [command, argument] = process.argv.slice(2);
switch (command) {
    case "sync-faq": syncFaq(); break;
    case "check": check(); break;
    case "set-email": setEmail(argument); break;
    default:
        console.error("Usage : node scripts/site.mjs sync-faq | check | set-email <adresse>");
        process.exit(2);
}
