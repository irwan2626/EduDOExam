package com.capstone.edudoexam.ui.dashboard.exams.detail.studens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.edudoexam.R
import com.capstone.edudoexam.components.DialogBottom
import com.capstone.edudoexam.components.FloatingMenu
import com.capstone.edudoexam.components.GenericListAdapter
import com.capstone.edudoexam.components.Snackbar
import com.capstone.edudoexam.components.UserDiffCallback
import com.capstone.edudoexam.components.Utils.Companion.dp
import com.capstone.edudoexam.components.Utils.Companion.getColor
import com.capstone.edudoexam.databinding.FragmentStudentsExamBinding
import com.capstone.edudoexam.databinding.ViewItemUserBinding
import com.capstone.edudoexam.databinding.ViewModalAddUserBinding
import com.capstone.edudoexam.models.User
import com.capstone.edudoexam.ui.dashboard.DashboardActivity
import com.capstone.edudoexam.ui.dashboard.exams.detail.DetailExamViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StudentsExamFragment(
    private var examId: String?
) : Fragment(),
    GenericListAdapter.ItemBindListener<User, ViewItemUserBinding> {

    private val binding: FragmentStudentsExamBinding by lazy {
        FragmentStudentsExamBinding.inflate(layoutInflater)
    }
    private val viewModel: DetailExamViewModel by viewModels()
    private val genericAdapter:  GenericListAdapter<User, ViewItemUserBinding> by lazy {
        GenericListAdapter(
            ViewItemUserBinding::class.java,
            onItemBindCallback = this,
            diffCallback = UserDiffCallback()
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding.apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = genericAdapter
            }
            floatingActionButton.setOnClickListener { addUserHandler() }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.apply {
            users.observe(viewLifecycleOwner) {
                lifecycleScope.launch {
                    delay(400)
                    genericAdapter.submitList(it)
                    setLoading(false)
                }
            }
        }
        lifecycleScope.launch {
            delay(400)
            fetchUsers()
        }
    }

    private fun fetchUsers() {
        Log.d("StudentsExamFragment", "fetchUsers: $examId")
        examId?.let { examId ->
            setLoading(true)
            viewModel.withUsers(requireActivity())
                .onError {
                    lifecycleScope.launch {
                        delay(400)
                        setLoading(false)
                        Snackbar.with(binding.root).show("Something went wrong", it.message, Snackbar.LENGTH_LONG)
                    }
                }
                .fetch { it.getStudents(examId) }
        }
    }

    private fun showItemMenu(item: User, anchor: View) {
        anchor.animate()
            .rotation(180f)
            .setDuration(150)
            .start()

        FloatingMenu(requireContext(), anchor).apply {

            val floatingMenu = this

            onDismissCallback = {
                anchor.animate()
                    .rotation(0f)
                    .setDuration(150)
                    .start()
            }

            xOffset = -300
            yOffset = 80

            addItem("Remove").apply {
                icon = ContextCompat.getDrawable(
                    context,
                    R.drawable.baseline_person_remove_24
                )
                color = getColor(context, R.color.danger)
                setOnClickListener {
                    floatingMenu.hide()
                    actionRemoveHandler(item)
                }
            }

            addItem("Block").apply {
                icon = ContextCompat.getDrawable(
                    context,
                    R.drawable.baseline_remove_circle_24
                )
                color = getColor(context, R.color.waring)
                setOnClickListener {
                    floatingMenu.hide()
                    actionBlockHandler(item)
                }
            }
        }.show()
    }

    private fun actionBlockHandler(item: User) {
        DialogBottom.Builder(requireActivity()).apply {
            color = getColor(requireContext(), R.color.waring)
            title = "Are you sure?"
            message = "Are you sure you want to block user from this exam?\nUser detail:\nName\t: ${item.name}\nEmail\t: ${item.email}\n"
            acceptText = "Block"
            acceptHandler = {

                true
            }
        }.show()
    }

    private fun actionRemoveHandler(item: User) {

        DialogBottom.Builder(requireActivity()).apply {
            color = getColor(requireContext(), R.color.danger)
            title = "Are you sure?"
            message = "Are you sure you want to remove user from this exam?\nUser detail:\nName\t: ${item.name}\nEmail\t: ${item.email}\nThis action cannot be undone."
            acceptText = "Remove"
            acceptHandler = {

                true
            }
        }.show()

    }

    private fun addUserHandler() {
        DialogBottom.Builder(requireActivity()).apply {
            title   = "Add User"
            message = "Please enter email user, make sure user has ben registered."
            setLayout(ViewModalAddUserBinding::class.java) { binding, dialog ->
                binding.apply {
                    inputEmail
                }
            }
            acceptText = "Add"
        }.show()
    }

    private fun setLoading(isLoading: Boolean) {
        (requireActivity() is DashboardActivity).apply {
            (requireActivity() as DashboardActivity).setLoading(isLoading)
        }
    }

    override fun onViewBind(binding: ViewItemUserBinding, item: User, position: Int) {

        binding.apply {
            userName.text = item.name
            userEmail.text = item.email

            root.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 18.dp)
                    layoutParams = this
                }
                actionButton.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { showItemMenu(item, actionButton) }
                }
            }
        }
    }

}