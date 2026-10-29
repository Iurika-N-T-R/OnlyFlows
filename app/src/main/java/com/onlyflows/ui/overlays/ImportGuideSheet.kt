package com.onlyflows.ui.overlays

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyflows.data.smartadd.ContentWriter
import com.onlyflows.data.smartadd.importFromText
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * How to import, for beginners: a prompt to copy into ChatGPT / Claude / Gemini so it writes
 * the file for you, then "Paste and import" to bring its answer in — no file to save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportGuideSheet(onDismiss: () -> Unit) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope     = rememberCoroutineScope()
    var summary   by remember { mutableStateOf<ContentWriter.Summary?>(null) }
    var importing by remember { mutableStateOf(false) }
    val prompt    = remember { aiPrompt(LocalDate.now()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("Guide d'import", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            Heading("Avec un assistant IA (le plus simple)")
            Step("1", "Copie le texte ci-dessous.")
            Step("2", "Colle-le dans ChatGPT, Claude, Gemini… puis ajoute à la suite tes notes, ta liste ou ton projet décrit en vrac.")
            Step("3", "Copie la réponse de l'assistant (le bloc de code).")
            Step("4", "Reviens ici et appuie sur « Coller et importer ».")
            Spacer(Modifier.height(10.dp))

            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Text(
                        prompt,
                        style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(12.dp).heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick  = {
                        clipboard.setText(AnnotatedString(prompt))
                        Toast.makeText(context, "Texte copié", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Copier")
                }
                Button(
                    onClick  = {
                        val pasted = clipboard.getText()?.text.orEmpty()
                        if (pasted.isBlank()) {
                            Toast.makeText(context, "Le presse-papiers est vide", Toast.LENGTH_SHORT).show()
                        } else if (pasted.contains("Tu prépares un fichier d'import")) {
                            Toast.makeText(context, "C'est le texte du guide : colle plutôt la réponse de l'assistant", Toast.LENGTH_LONG).show()
                        } else {
                            importing = true
                            scope.launch { summary = importFromText(context, pasted); importing = false }
                        }
                    },
                    enabled  = !importing,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.ContentPaste, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (importing) "Import…" else "Coller et importer")
                }
            }
            summary?.let { Spacer(Modifier.height(10.dp)); ImportSummaryCard(it) }

            Spacer(Modifier.height(20.dp))
            Heading("Ou écris le fichier toi-même")
            Body("Settings → « Import tasks, projects & lists » lit les fichiers .txt, .md, .csv et .json. Réimporter le même fichier ne crée pas de doublons : ce qui existe est complété.")
            Spacer(Modifier.height(8.dp))
            Body("Le plus simple, un fichier texte :")
            Code(
                """
                Appeler le dentiste demain à 10h

                # Voyage à Rome
                - Réserver les vols vendredi prochain !!
                - [x] Refaire le passeport
                - Faire la valise
                  - Chargeur
                  - Adaptateur

                # Courses
                - Lait
                - Pain

                # Notes
                Musées
                Réserver le Vatican en ligne
                """.trimIndent(),
            )
            Body("• # Titre = un projet (« Courses », « Liste » → checklist ; « Notes », « Idées » → notes)\n• ## Titre = une section\n• - texte = une tâche, - [x] = faite, puce décalée = sous-étape\n• dates, heures, #étiquettes et !! (priorité) sont compris")
            Spacer(Modifier.height(8.dp))
            Body("En tableur (CSV), une ligne d'en-tête puis une tâche par ligne :")
            Code("Titre;Projet;Échéance;Priorité;Terminé;Étiquettes;Sous-tâches\nChoisir le carrelage;Rénovation;12/11/2026;haute;non;maison;Devis|Échantillons")
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable private fun Heading(s: String) = Text(s, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
@Composable private fun Body(s: String) = Text(s, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun Step(n: String, s: String) = Row(Modifier.padding(vertical = 2.dp)) {
    Text("$n.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(22.dp))
    Text(s, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Code(s: String) = Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
    SelectionContainer { Text(s, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.padding(10.dp)) }
}

/** The text to paste into an AI assistant so it answers with JSON this importer reads. */
private fun aiPrompt(today: LocalDate): String {
    val date = today.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH))
    return """
Tu prépares un fichier d'import pour mon application de tâches OnlyFlows.
À partir de ce que je te donne à la fin (notes, liste, projet décrit en vrac…), réponds UNIQUEMENT par un bloc de code JSON valide, sans texte autour, avec cette structure :

{
  "projects": [
    {
      "title": "Nom du projet",
      "description": "facultatif",
      "deadline": "AAAA-MM-JJ",
      "sections": [ { "title": "Nom de section", "tasks": [ TÂCHE, … ] } ],
      "tasks": [ TÂCHE, … ]
    }
  ],
  "lists": [ { "title": "Courses", "items": ["Lait", "Pain"] } ],
  "notebooks": [ { "title": "Idées", "notes": [ { "title": "…", "content": "…" } ] } ],
  "tasks": [ TÂCHE, … ]
}

Une TÂCHE :
{
  "title": "obligatoire, court, commence par un verbe",
  "description": "facultatif",
  "due": "AAAA-MM-JJ ou AAAA-MM-JJTHH:MM",
  "priority": "high | medium | low",
  "done": false,
  "checklist": ["sous-étape 1", "sous-étape 2"],
  "tags": ["étiquette"],
  "repeat": "every day | every monday | every 2 weeks | every month",
  "reminder": "AAAA-MM-JJTHH:MM"
}

Règles :
- "projects" : ce qui demande plusieurs étapes. "lists" : les simples listes à cocher (courses, valise…). "notebooks" : les idées et informations à garder. "tasks" : les tâches isolées.
- Mets seulement les champs que tu connais. Supprime les blocs vides. N'invente ni dates ni priorités.
- Nous sommes le $date : transforme « demain », « vendredi prochain »… en vraies dates.
- Découpe les gros objectifs en tâches concrètes ; les étapes d'une tâche vont dans "checklist".
- Garde la langue de mes notes pour les titres.

Voici mes éléments :

""".trimStart()
}
