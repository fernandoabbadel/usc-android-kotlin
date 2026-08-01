@file:OptIn(ExperimentalLayoutApi::class)

package com.example.usc1.ui.cadastro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.CadastroChoiceOption
import com.example.usc1.domain.model.CadastroDefaults

/**
 * Clone Compose de `web-reference/src/app/cadastro/page.tsx`:
 * identidade, contato com toggles de privacidade, origem, astro e preferencias.
 */
@Composable
fun CadastroScreen(
    state: CadastroUiState,
    onFormChange: (com.example.usc1.domain.model.CadastroForm) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando cadastro", modifier = modifier)
        return
    }

    val form = state.form

    PremiumScreen(modifier = modifier, bottomPadding = 120.dp) {
        PremiumHeader(
            title = state.title,
            subtitle = "Dados que aparecem no seu perfil e no álbum",
            icon = Icons.Outlined.Person,
            onBackClick = onBackClick,
        )

        // Cabecalho com foto atual.
        PremiumCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(66.dp),
                    shape = CircleShape,
                    color = PremiumZinc900,
                    border = BorderStroke(2.dp, PremiumBrand),
                ) {
                    if (form.foto.isNotBlank()) {
                        AsyncImage(
                            model = form.foto,
                            contentDescription = "Sua foto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = form.nome.take(1).uppercase().ifBlank { "U" },
                                color = PremiumBrand,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = form.nome.ifBlank { "Seu nome" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "A foto vem da sua conta Google.",
                        color = PremiumZinc500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        CadastroSectionTitle("Identidade")
        PremiumCard {
            PremiumTextField(
                value = form.nome,
                onValueChange = { onFormChange(form.copy(nome = it)) },
                label = "Nome completo",
                leadingIcon = Icons.Outlined.Person,
            )
            PremiumTextField(
                value = form.apelido,
                onValueChange = { onFormChange(form.copy(apelido = it)) },
                label = "Apelido (aparece no álbum)",
                leadingIcon = Icons.Outlined.Badge,
            )
            PremiumTextField(
                value = form.matricula,
                onValueChange = { onFormChange(form.copy(matricula = it)) },
                label = "Matrícula",
                leadingIcon = Icons.Outlined.Badge,
            )
            PremiumTextField(
                value = form.bio,
                onValueChange = { onFormChange(form.copy(bio = it.take(240))) },
                label = "Bio",
                singleLine = false,
                leadingIcon = Icons.Outlined.Chat,
            )
        }

        CadastroSectionTitle("Turma")
        PremiumCard {
            if (state.config.turmas.isEmpty()) {
                PremiumEmptyState(
                    title = "Nenhuma turma publicada",
                    subtitle = "A atlética ainda não configurou as turmas neste tenant.",
                    icon = Icons.Outlined.Badge,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.config.turmas.forEach { turma ->
                        CadastroSelectableChip(
                            label = turma.label,
                            selected = form.turma.equals(turma.id, ignoreCase = true),
                            onClick = { onFormChange(form.copy(turma = turma.id)) },
                        )
                    }
                }
            }
        }

        CadastroSectionTitle("Contato e privacidade")
        PremiumCard {
            PremiumTextField(
                value = form.instagram,
                onValueChange = { onFormChange(form.copy(instagram = it.removePrefix("@"))) },
                label = "Instagram (sem @)",
                leadingIcon = Icons.Outlined.AlternateEmail,
            )
            CadastroPrivacyToggle(
                label = "Mostrar Instagram no perfil",
                checked = form.instagramPublico,
                onCheckedChange = { onFormChange(form.copy(instagramPublico = it)) },
            )
            PremiumTextField(
                value = form.telefone,
                onValueChange = { onFormChange(form.copy(telefone = it)) },
                label = "Telefone / WhatsApp",
                leadingIcon = Icons.Outlined.Phone,
            )
            CadastroPrivacyToggle(
                label = "Mostrar WhatsApp no perfil",
                checked = form.whatsappPublico,
                onCheckedChange = { onFormChange(form.copy(whatsappPublico = it)) },
            )
        }

        CadastroSectionTitle("Origem e nascimento")
        PremiumCard {
            PremiumTextField(
                value = form.cidadeOrigem,
                onValueChange = { onFormChange(form.copy(cidadeOrigem = it)) },
                label = "Cidade de origem",
                leadingIcon = Icons.Outlined.LocationCity,
            )
            PremiumTextField(
                value = form.estadoOrigem,
                onValueChange = { onFormChange(form.copy(estadoOrigem = it.uppercase().take(2))) },
                label = "UF",
                leadingIcon = Icons.Outlined.LocationCity,
            )
            PremiumTextField(
                value = form.dataNascimento,
                onValueChange = { onFormChange(form.copy(dataNascimento = it)) },
                label = "Data de nascimento (AAAA-MM-DD)",
                leadingIcon = Icons.Outlined.Cake,
            )
            CadastroPrivacyToggle(
                label = "Mostrar idade no perfil",
                checked = form.idadePublica,
                onCheckedChange = { onFormChange(form.copy(idadePublica = it)) },
            )
        }

        CadastroSectionTitle("Relacionamento")
        PremiumCard {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.config.relationshipOptions.forEach { option ->
                    CadastroSelectableChip(
                        label = option,
                        selected = form.statusRelacionamento.equals(option, ignoreCase = true),
                        onClick = { onFormChange(form.copy(statusRelacionamento = option)) },
                    )
                }
            }
            CadastroPrivacyToggle(
                label = "Mostrar status de relacionamento",
                checked = form.relacionamentoPublico,
                onCheckedChange = { onFormChange(form.copy(relacionamentoPublico = it)) },
            )
        }

        CadastroSectionTitle("Astro")
        PremiumCard {
            Text(
                text = "SIGNO",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CadastroDefaults.zodiacSigns.forEach { sign ->
                    CadastroSelectableChip(
                        label = sign,
                        selected = form.signo.equals(sign, ignoreCase = true),
                        onClick = {
                            onFormChange(form.copy(signo = if (form.signo == sign) "" else sign))
                        },
                    )
                }
            }
            CadastroPrivacyToggle(
                label = "Mostrar signo no perfil",
                checked = form.signoPublico,
                onCheckedChange = { onFormChange(form.copy(signoPublico = it)) },
            )
            PremiumTextField(
                value = form.ascendente,
                onValueChange = { onFormChange(form.copy(ascendente = it)) },
                label = "Ascendente",
                leadingIcon = Icons.Outlined.AutoAwesome,
            )
            CadastroPrivacyToggle(
                label = "Mostrar ascendente no perfil",
                checked = form.ascendentePublico,
                onCheckedChange = { onFormChange(form.copy(ascendentePublico = it)) },
            )
        }

        CadastroSectionTitle("Esportes")
        PremiumCard {
            CadastroMultiChoice(
                options = state.config.sportOptions,
                selected = form.esportes,
                onToggle = { id -> onFormChange(form.copy(esportes = form.esportes.toggled(id))) },
            )
        }

        CadastroSectionTitle("Lugares favoritos")
        PremiumCard {
            CadastroMultiChoice(
                options = state.config.specialPlaceOptions,
                selected = form.lugarEspecial,
                onToggle = { id ->
                    onFormChange(form.copy(lugarEspecial = form.lugarEspecial.toggled(id)))
                },
            )
        }

        CadastroSectionTitle("Comidas favoritas")
        PremiumCard {
            CadastroMultiChoice(
                options = state.config.foodOptions,
                selected = form.comidaPreferida,
                onToggle = { id ->
                    onFormChange(form.copy(comidaPreferida = form.comidaPreferida.toggled(id)))
                },
            )
        }

        CadastroSectionTitle("Músicas favoritas")
        PremiumCard {
            CadastroMultiChoice(
                options = state.config.musicOptions,
                selected = form.musicaPreferida,
                onToggle = { id ->
                    onFormChange(form.copy(musicaPreferida = form.musicaPreferida.toggled(id)))
                },
            )
        }

        CadastroSectionTitle("Cor preferida")
        PremiumCard {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.config.colorOptions.forEach { color ->
                    val selected = form.corPreferida.equals(color.id, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(color.hex))
                            .clickable {
                                onFormChange(
                                    form.copy(corPreferida = if (selected) "" else color.id),
                                )
                            }
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, PremiumBrand, CircleShape)
                                } else {
                                    Modifier.border(1.dp, PremiumZinc700, CircleShape)
                                },
                            ),
                    )
                }
            }
        }

        CadastroSectionTitle("Pets")
        PremiumCard {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.config.petOptions.forEach { pet ->
                    CadastroSelectableChip(
                        label = "${pet.icon} ${pet.label}",
                        selected = form.pets.equals(pet.id, ignoreCase = true),
                        onClick = { onFormChange(form.copy(pets = pet.id)) },
                    )
                }
            }
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFFF59E0B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        state.savedMessage?.let { message ->
            Text(
                text = message,
                color = PremiumBrand,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        PremiumPrimaryButton(
            text = if (state.isSaving) "Salvando..." else "Salvar cadastro",
            onClick = onSaveClick,
            enabled = state.canSave,
        )
    }
}

@Composable
private fun CadastroSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = PremiumZinc500,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 2.dp),
    )
}

@Composable
private fun CadastroMultiChoice(
    options: List<CadastroChoiceOption>,
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            CadastroSelectableChip(
                label = "${option.icon} ${option.label}",
                selected = selected.any { it.equals(option.id, ignoreCase = true) },
                onClick = { onToggle(option.id) },
            )
        }
    }
}

@Composable
private fun CadastroSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) PremiumBrand.copy(alpha = 0.18f) else PremiumZinc900,
        border = BorderStroke(1.dp, if (selected) PremiumBrand else PremiumZinc800),
    ) {
        Text(
            text = label,
            color = if (selected) PremiumBrand else PremiumZinc300,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun CadastroPrivacyToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                tint = if (checked) PremiumBrand else PremiumZinc700,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                color = PremiumZinc400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PremiumBrand,
                uncheckedThumbColor = PremiumZinc400,
                uncheckedTrackColor = PremiumZinc800,
            ),
        )
    }
}

private fun List<String>.toggled(id: String): List<String> =
    if (any { it.equals(id, ignoreCase = true) }) {
        filterNot { it.equals(id, ignoreCase = true) }
    } else {
        this + id
    }

private fun parseHexColor(hex: String): Color {
    val clean = hex.trim().removePrefix("#")
    val value = clean.toLongOrNull(16) ?: return PremiumZinc700
    return when (clean.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> PremiumZinc700
    }
}
