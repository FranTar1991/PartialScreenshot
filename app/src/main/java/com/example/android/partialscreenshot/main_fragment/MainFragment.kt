package com.example.android.partialscreenshot.main_fragment

import android.content.DialogInterface
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotsDatabase
import com.example.android.partialscreenshot.databinding.FragmentMainBinding
import com.example.android.partialscreenshot.main_fragment.adapter.MyItemDetailsLookup
import com.example.android.partialscreenshot.main_fragment.adapter.MyItemKeyProvider
import com.example.android.partialscreenshot.main_fragment.adapter.ScreenshotListener
import com.example.android.partialscreenshot.main_fragment.adapter.ScreenshotsAdapter
import com.example.android.partialscreenshot.utils.MainActivityViewModel
import android.widget.Toast

import android.view.*

import androidx.recyclerview.selection.*
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.example.android.partialscreenshot.MainActivity
import com.example.android.partialscreenshot.utils.createActionDialog
import com.example.android.partialscreenshot.utils.deleteItemFromGallery


class MainFragment : Fragment() {


    private var allSelected: Boolean = false

    private var uriList: MutableList<String> = mutableListOf()
    private lateinit var adapter: ScreenshotsAdapter
    private lateinit var screenshotsSelected: Selection<String>
    private lateinit var tracker: SelectionTracker<String>
    private var actionMode: ActionMode? = null
    private lateinit var mainFragmentViewModel: MainFragmentViewModel
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.menu_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {

            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.delete_on_menu -> {
                    getUserAuthorizationToTakeAction(item.itemId, ::deleteThisItems)
                    true
                }
                R.id.share_on_menu -> {
                    getUserAuthorizationToTakeAction(item.itemId, ::shareThisItem)
                    true
                }
                R.id.select_all_on_menu -> {

                    allSelected = if (!allSelected){
                        uriList.forEach {
                            if (!tracker.isSelected(it)){
                                tracker.select(it)
                            }
                        }
                        true
                    } else {
                        uriList.forEach {
                            tracker.deselect(it)
                        }
                        false
                    }


                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {

            tracker.clearSelection()
            actionMode = null
        }
    }

    private fun getUserAuthorizationToTakeAction(id: Int, actionToTake: () -> Unit) {

        val (title, message) = when (id){
            R.id.delete_on_menu-> Pair(getString(R.string.delete_this),getString(R.string.delete_this_message))
            R.id.share_on_menu -> Pair(getString(R.string.share_this), getString(R.string.share_this_message))
            else -> Pair("","")
        }

        createActionDialog(actionToTake, activity as MainActivity,title, message, actionMode)
    }

    private fun deleteThisItems(){
        val list = screenshotsSelected.toList()
        mainFragmentViewModel.onDeleteListWithUri(list)
        deleteItemFromGallery(screenshotsSelected.toList(), context?.contentResolver)
        Toast.makeText(context, getString(R.string.delete,screenshotsSelected.size()), Toast.LENGTH_SHORT).show()
    }

    private fun shareThisItem(){
        val files: ArrayList<Uri> = ArrayList<Uri>()
        uriList.forEach {
            files.add(Uri.parse(it))
        }

        val sendIntent = Intent().apply {
            type = "image/jpeg"
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
        }

        val shareIntent = Intent.createChooser(sendIntent, getText(R.string.share))

        startActivity(shareIntent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentMainBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_main, container, false)

        binding.callFloatingWindow.setOnClickListener(View.OnClickListener {
            mainActivityViewModel.checkIfHasOverlayPermission(true)
        })

        val application = requireNotNull(this.activity).application
        val dataSource = ScreenshotsDatabase.getInstance(application).screenshotsDAO
        val viewModelFactory = MainFragmentViewmodelFactory(dataSource, application)


        mainFragmentViewModel =
            ViewModelProvider(this, viewModelFactory).get(MainFragmentViewModel::class.java)


        binding.allScreenshotsViewModel = mainFragmentViewModel
        binding.lifecycleOwner = activity

        adapter = ScreenshotsAdapter(ScreenshotListener(::clickListener), mainFragmentViewModel)

        binding.allPictures.adapter = adapter

        val manager = GridLayoutManager(activity,3)
        binding.allPictures.layoutManager = manager


        mainFragmentViewModel.screenshots.observe(viewLifecycleOwner, Observer {
            it?.let { newList ->

                newList.forEach { list->
                    uriList.add(list.uri)
                }
                adapter.submitList(newList)
                mainFragmentViewModel.setScreenShotCount(newList.size)

            }
        })



        mainFragmentViewModel.navigateToScreenshot.observe(viewLifecycleOwner, Observer { screenshot ->
            screenshot?.let {
                actionMode?.finish()
                this.findNavController()
                    .navigate(MainFragmentDirections.actionMainFragmentToDetailsFragment(screenshot))
                mainFragmentViewModel.onScreenshotNavigated()
            }
        })

        tracker = SelectionTracker.Builder<String>(
            "mySelection",
            binding.allPictures,
           MyItemKeyProvider(adapter),
            MyItemDetailsLookup(binding.allPictures),
           StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        )
           .build()
        adapter.tracker = tracker

        tracker.addObserver(
            object : SelectionTracker.SelectionObserver<String>() {
                override fun onSelectionRestored() {
                    super.onSelectionRestored()
                  setActionMode()
                }

                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    setActionMode()
                }

            })


        return binding.root
    }


    private fun setActionMode() {
        screenshotsSelected = tracker.selection
        val items = screenshotsSelected.size()

        when (actionMode) {
            null -> {
                // Start the CAB using the ActionMode.Callback defined above
                actionMode = activity?.startActionMode(actionModeCallback)

            }
        }
        actionMode?.title = getString(R.string.items_selected, items)


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tracker.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tracker.onRestoreInstanceState(savedInstanceState)
    }

    private fun clickListener(uri: String){
        mainFragmentViewModel.onScreenshotClicked(uri)
    }



}